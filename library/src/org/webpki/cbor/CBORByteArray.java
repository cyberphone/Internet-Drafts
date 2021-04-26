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
package org.webpki.cbor;

import java.io.IOException;

/**
 * Class for holding CBOR byte arrays.
 */
public class CBORByteArray extends CBORObject {

    private static final long serialVersionUID = 1L;

    byte[] byteArray;

    public CBORByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }
    
    @Override
    public CBORTypes getType() {
        return CBORTypes.BYTE_ARRAY;
    }

    @Override
    public byte[] writeObject() throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    StringBuilder internalToString() {
        // TODO Auto-generated method stub
        return null;
    }
}
