/*
 *  Copyright 2006-2020 WebPKI.org (http://webpki.org).
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
package org.webpki.crypto;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.HashMap;

import org.webpki.asn1.ASN1Integer;
import org.webpki.asn1.ASN1ObjectID;
import org.webpki.asn1.ASN1OctetString;
import org.webpki.asn1.ASN1Sequence;
import org.webpki.asn1.BaseASN1Object;
import org.webpki.asn1.DerDecoder;
import org.webpki.asn1.ParseUtil;

import org.webpki.util.ArrayUtil;
import org.webpki.util.DebugFormatter;

/**
 * Support methods for "OKP" (RFC 8037)
 */
public class CryptoUtil {
    
    private CryptoUtil() {}
    
    static final HashMap<KeyAlgorithms,Integer> okpKeyLength = new HashMap<>();

    static {
        okpKeyLength.put(KeyAlgorithms.ED25519, 32);
        okpKeyLength.put(KeyAlgorithms.ED448,   57);
        okpKeyLength.put(KeyAlgorithms.X25519,  32);
        okpKeyLength.put(KeyAlgorithms.X448,    56);
    }

    public static byte[] public2RawOkpKey(PublicKey publicKey, KeyAlgorithms keyAlgorithm)
    throws IOException {
        byte[] encoded = publicKey.getEncoded();
        if (okpKeyLength.get(keyAlgorithm) != encoded.length - 12) {
            throw new IOException("Wrong public key length for: " + keyAlgorithm.toString());
        }
        byte[] rawKey = new byte[encoded.length - 12];
        System.arraycopy(encoded, 12, rawKey, 0, rawKey.length);
        return rawKey;
    }

    static final HashMap<KeyAlgorithms,byte[]> okpPrefix = new HashMap<>();

    static {
        try {
            okpPrefix.put(KeyAlgorithms.ED25519, 
                          DebugFormatter.getByteArrayFromHex("302a300506032b6570032100"));
            okpPrefix.put(KeyAlgorithms.ED448,
                          DebugFormatter.getByteArrayFromHex("3043300506032b6571033a00"));
            okpPrefix.put(KeyAlgorithms.X25519,
                          DebugFormatter.getByteArrayFromHex("302a300506032b656e032100"));
            okpPrefix.put(KeyAlgorithms.X448,
                          DebugFormatter.getByteArrayFromHex("3042300506032b656f033900"));
        } catch (IOException e) {
        }
    }

    public static PublicKey raw2PublicOkpKey(byte[] x, KeyAlgorithms keyAlgorithm) 
    throws GeneralSecurityException {
//#if BC
        return KeyFactory.getInstance(keyAlgorithm.getJceName(), "BC")
//#else
        return KeyFactory.getInstance(keyAlgorithm.getJceName())
//#endif
                .generatePublic(
                        new X509EncodedKeySpec(
                                ArrayUtil.add(okpPrefix.get(keyAlgorithm), x)));
    }

    public static byte[] private2RawOkpKey(PrivateKey privateKey, KeyAlgorithms keyAlgorithm) 
    throws IOException {
        byte[] rawKey = ParseUtil.octet(
                DerDecoder.decode(
                        ParseUtil.octet(
                                ParseUtil.sequence(
                                        DerDecoder.decode(privateKey.getEncoded())).get(2))));
        if (okpKeyLength.get(keyAlgorithm) != rawKey.length) {
            throw new IOException("Wrong private key length for: " + keyAlgorithm.toString());
        }
        return rawKey;
    }

    public static PrivateKey raw2PrivateOkpKey(byte[] d, KeyAlgorithms keyAlgorithm)
    throws IOException, GeneralSecurityException {
//#if BC
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm.getJceName(), "BC");
//#else
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgorithm.getJceName());
//#endif
        byte[] pkcs8 = new ASN1Sequence(new BaseASN1Object[] {
            new ASN1Integer(0),
            new ASN1Sequence(new ASN1ObjectID(keyAlgorithm.getECDomainOID())),
            new ASN1OctetString(new ASN1OctetString(d).encode())
        }).encode();
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
    }
}
