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
import java.security.GeneralSecurityException;

/**
 * Class for holding CBOR integer maps.
 */
public class CBORIntegerMap extends CBORMapBase {

    public CBORIntegerMap() {}

    /**
     * Set map value.
     * 
     * @param key The key
     * @param value The value expressed as a CBOR object
     * @return The CBORIntegerMap
     * @throws IOException
     */
    public CBORIntegerMap setMappedValue(int key, CBORObject value) throws IOException {
        setObject(new CBORInteger(key), value);
        return this;
    }

    /**
     * Validate signed CBOR object.
     * 
     * @param key Of map to validate
     * @param validator Holds the validation method
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public void validate(int key, CBORValidator validator) 
            throws IOException, GeneralSecurityException {
        validate(new CBORInteger(key), validator);
    }

    /**
     * Sign CBOR object.
     * 
     * @param key Of the map to sign
     * @param signer Holder of signature method and key
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public CBORIntegerMap sign(int key, CBORSigner signer) 
            throws IOException, GeneralSecurityException {
        sign(new CBORInteger(key), signer);
        return this;
    }

    /**
     * Check map for key presence.
     * 
     * @param key The key
     * @return <code>true</code> if the key is present
     */
    public boolean hasKey(int key) {
        return hasKey(new CBORInteger(key));
    }

    /**
     * Get map value.
     * 
     * @param key
     * @return The CBORObject
     * @throws IOException
     */
    public CBORObject getMappedValue(int key) throws IOException {
        return getObject(new CBORInteger(key));
    }
}
