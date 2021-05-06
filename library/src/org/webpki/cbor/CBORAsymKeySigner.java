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
import java.security.PrivateKey;
import java.security.PublicKey;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.SignatureWrapper;

/**
 * Class for creating CBOR asymmetric key signatures.
 * 
 * Note that signer objects may be used any number of times
 * (assuming that the same parameters are valid).  They are also
 * thread-safe.
 */
public class CBORAsymKeySigner extends CBORSigner {

    PrivateKey privateKey;

    AsymSignatureAlgorithms signatureAlgorithm;

    /**
     * Initialize signer.
     * 
     * @param privateKey The key to sign with
     * @param signatureAlgorithm The algorithm to use
     */
    public CBORAsymKeySigner(PrivateKey privateKey,
                             AsymSignatureAlgorithms signatureAlgorithm) {
        this.privateKey = privateKey;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cborAlgorithmId = WEBPKI_2_CBOR_ALG.get(signatureAlgorithm);
    }
    
    /**
     * Initialize signer.
     * 
     * The default signature algorithm to use is based on the recommendations
     * in RFC 7518.
     * 
     * @param privateKey The key to sign with
     * @throws IOException 
     */
    public CBORAsymKeySigner(PrivateKey privateKey) throws IOException {
        this(privateKey,
             KeyAlgorithms.getKeyAlgorithm(privateKey).getRecommendedSignatureAlgorithm());
    }

    /**
     * Put a public into the signature container.
     * 
     * @param publicKey The public key
     * @return this
     */
    public CBORAsymKeySigner setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
        return this;
    }

    @Override
    byte[] signData(byte[] dataToBeSigned) throws GeneralSecurityException, IOException {
        return new SignatureWrapper(signatureAlgorithm, privateKey, provider)
                .update(dataToBeSigned)
                .sign();
    }
}
