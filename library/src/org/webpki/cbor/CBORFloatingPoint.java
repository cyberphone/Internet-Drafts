/*
 *  Copyright 2006-2021 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.cbor;

import java.io.IOException;

/**
 * Class for holding CBOR floating point numbers.
 * 
 * Numbers are constrained to the IEEE 754 notation
 * using the length 16, 32, and 64 bit on "wire".  Which
 * length to use is governed by the size and precision 
 * required to (minimally) correctly represent a number.
 * API-wise numbers are only communicated as
 * 64-bit items (Java double).
 */
public class CBORFloatingPoint extends CBORObject {

    double value;
    
    byte tag = MT_FLOAT16;
    long bitFormat;
    
    /**
     * Create a CBOR <code>floating point</code> object.
     * 
     * @param value
     */
    public CBORFloatingPoint(double value) {
        this.value = value;
        bitFormat = Double.doubleToLongBits(value);
        if (bitFormat == FLOAT64_POS_ZERO) {
            bitFormat = FLOAT16_POS_ZERO;
        } else if (bitFormat == FLOAT64_NEG_ZERO) {
            bitFormat = FLOAT16_NEG_ZERO;
        } else if ((bitFormat & FLOAT64_POS_INFINITY) == FLOAT64_POS_INFINITY) {
            // Special "number"
            if (bitFormat == FLOAT64_POS_INFINITY) {
                bitFormat = FLOAT16_POS_INFINITY;
            } else if (bitFormat == FLOAT64_NEG_INFINITY) {
                bitFormat = FLOAT16_NEG_INFINITY;
            } else {
                // Due to the deterministic encoding there is no support for "signaling"
                bitFormat = FLOAT16_NOT_A_NUMBER;
            }
        } else if (Math.abs(value) > Float.MAX_VALUE || value != (double)((float) value)) {
            // Too big or would lose precision unless we stick to 64 bits.
            tag = MT_FLOAT64; 
        } else { 
            // Assumption: we go for 32 bits until proven wrong :)
            int float32 = Float.floatToIntBits((float)value);
            tag = MT_FLOAT32;
            bitFormat = float32 & 0xffffffffl;
            int actualExponent = ((float32 >>> FLOAT32_FRACTION_SIZE) & 
                ((1 << FLOAT32_EXPONENT_SIZE) - 1)) - FLOAT32_EXPONENT_BIAS;
            if (actualExponent == -FLOAT32_EXPONENT_BIAS) {
                // Unnormalized float32 will not translate to float16
                return;
            }
            if (actualExponent > FLOAT16_EXPONENT_BIAS) {
                // To big for float16 and all bits set are reserved for NaN and Infinity
                return;
            }
            int frac16 = (float32 >> (FLOAT32_FRACTION_SIZE - FLOAT16_FRACTION_SIZE)) & 
                    ((1 << FLOAT16_FRACTION_SIZE) - 1);
            if ((float32 & ((1 << FLOAT32_FRACTION_SIZE) - 1)) != 
                    (frac16 << (FLOAT32_FRACTION_SIZE - FLOAT16_FRACTION_SIZE))) {
                // Losing fraction bits is not an option
                return;
            }

            // Add proper float16 exponent bias
            int exp16 = actualExponent + FLOAT16_EXPONENT_BIAS;

            // Check if we need to unnormalize data
            if (exp16 <= 0) {
                // The implicit "1" becomes explicit using unnormalized representation
                frac16 += 1 << FLOAT16_FRACTION_SIZE;
                exp16--;
                // Always do at least one turn
                do {
                    if ((frac16 & 1) != 0) {
                        // Too off scale for float16
                        return;
                    }
                    frac16 >>= 1;
                } while (++exp16 < 0);
            }

            // Seems like 16 bits indeed are sufficient!
            tag = MT_FLOAT16;
            bitFormat = 
               // Sign bit
               ((float32 >>> 16) & 0x8000) +
               // Exponent.  Put it in front of fraction.
               (exp16 << FLOAT16_FRACTION_SIZE) +
               // Fraction
               frac16;
        }
    }

    /**
     * A slightly nicer formatter than Java's original
     * 
     * @param value The double
     * @return The double in string format
     */
    public static String formatDouble(double value) {
        return Double.toString(value).replace('E', 'e').replaceAll("e(\\d)", "e+$1");
    }

    @Override
    CBORTypes internalGetType() {
        return CBORTypes.FLOATING_POINT;
    }
    
    @Override
    byte[] internalEncode() throws IOException {
        int length = (2 << (tag - MT_FLOAT16)) + 1;
        byte[] encoded = new byte[length];
        encoded[0] = tag;
        long copy = bitFormat;
        while (--length > 0) {
            encoded[length] = (byte) copy;
            copy >>>= 8;
        }
        return encoded;
    }
    
    @Override
    void internalToString(CBORObject.PrettyPrinter prettyPrinter) {
         prettyPrinter.appendText(formatDouble(value));
    }
}