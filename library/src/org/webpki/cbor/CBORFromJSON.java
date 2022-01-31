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
 * Class for converting JSON to CBOR.
 * 
 * Note that the JSON Number type is restricted to integers with a magnitude <= 2^53.
 * 
 */
public class CBORFromJSON {

    char[] json;
    int index;

    // 2^53 ("53-bit precision")
    static final long MAX_JSON_INTEGER = 9007199254740992l;

    CBORFromJSON(String json) {
        this.json = json.toCharArray();
    }
    
    /**
     * Convert JSON to CBOR.
     * 
     * @param json JSON String
     * @return CBORObject
     * @throws IOException
     */
    public static CBORObject convert(String json) throws IOException {
        return new CBORFromJSON(json).readToEOF();
    }

    private void reportError(String error) throws IOException {
        // Unsurprisingly, error handling turned out to be the most complex part...
        int start = index - 100;
        if (start < 0) {
            start = 0;
        }
        int linePos = 0;
        while (start < index - 1) {
            if (json[start++] == '\n') {
                linePos = start;
            }
        }
        StringBuilder complete = new StringBuilder();
        if (index > 0 && json[index - 1] == '\n') {
            index--;
        }
        int endLine = index;
        while (endLine < json.length) {
            if (json[endLine] == '\n') {
                break;
            }
            endLine++;
        }
        for (int q = linePos; q < endLine; q++) {
            complete.append(json[q]);
        }
        complete.append('\n');
        for (int q = linePos; q < index; q++) {
            complete.append('-');
        }
        int lineNumber = 1;
        for (int q = 0; q < index - 1; q++) {
            if (json[q] == '\n') {
                lineNumber++;
            }
        }
        throw new IOException(complete.append("^\n\nError in line ")
                                      .append(lineNumber)
                                      .append(". ")
                                      .append(error).toString());
    }
    
    private CBORObject readToEOF() throws IOException {
        CBORObject cborObject = getObject();
        if (index < json.length) {
            readChar();
            reportError("Unexpected data after token");
        }
        return cborObject;
    }

    private CBORObject getObject() throws IOException {
        scanNonSignficantData();
        CBORObject cborObject = getRawObject();
        scanNonSignficantData();
        return cborObject;
    }
    
    private boolean continueList(char validStop) throws IOException {
        if (nextChar() == ',') {
            readChar();
            scanNonSignficantData();
            return true;
        }
        scanFor(String.valueOf(validStop));
        index--;
        return false;
    }
    
    private CBORObject getRawObject() throws IOException {
        switch (readChar()) {
        
            case '[':
                CBORArray array = new CBORArray();
                scanNonSignficantData();
                while (readChar() != ']') {
                    index--;
                    do {
                        array.addObject(getObject());
                    } while (continueList(']'));
                }
                return array;
     
            case '{':
                CBORMap map = new CBORMap();
                scanNonSignficantData();
                while (readChar() != '}') {
                    index--;
                    do {
                        if (nextChar() != '"') {
                            reportError("String expected");
                        }
                        CBORObject key = getObject();
                        scanFor(":");
                        map.setObject(key, getObject());
                    } while (continueList('}'));
                }
                return map;
       
            case '"':
                return getTextString();
                
            case 't':
                scanFor("rue");
                return new CBORBoolean(true);
       
            case 'f':
                scanFor("alse");
                return new CBORBoolean(false);
       
            case 'n':
                scanFor("ull");
                return new CBORNull();

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

            case '+':
            case '-':
                return getNumber();

            default:
                index--;
                reportError(String.format("Unexpected character: %s", toChar(readChar())));
                return null;  // For the compiler...
        }
    }

    private CBORObject getNumber() throws IOException {
        StringBuilder token = new StringBuilder();
        index--;
        char c;
        do  {
            token.append(readChar());
            c = nextChar();
        } while ((c >= '0' && c <= '9') || c == '.');
        try {
            long value = Long.valueOf(token.toString());
            if (Math.abs(value) > MAX_JSON_INTEGER) {
                reportError("JSON integer exceeded 2^53");
            }
            return new CBORInteger(value);
        } catch (IllegalArgumentException e) {
            reportError(e.getMessage());
        }
        return null; // For the compiler...
    }

    private char nextChar() throws IOException {
        if (index == json.length) return 0;
        char c = readChar();
        index--;
        return c;
    }

    private String toChar(char c) {
        return c < ' ' ? String.format("\\u%04x", (int) c) : String.format("'%c'", c);
    }

    private void scanFor(String expected) throws IOException {
        for (char c : expected.toCharArray()) {
            char actual = readChar(); 
            if (c != actual) {
                reportError(String.format("Expected: '%c' actual: %s", c, toChar(actual)));
            }
        }
    }

    private CBORObject getTextString() throws IOException {
        StringBuilder s = new StringBuilder();
        while (true) {
            char c;
            switch (c = readChar()) {
                case '\\':
                    switch (c = readChar()) {
                        case '"':
                        case '\\':
                            break;
    
                        case 'b':
                            c = '\b';
                            break;
    
                        case 'f':
                            c = '\f';
                            break;
    
                        case 'n':
                            c = '\n';
                            break;
    
                        case 'r':
                            c = '\r';
                            break;
    
                        case 't':
                            c = '\t';
                            break;
    
                        case 'u':
                            c = 0;
                            for (int i = 0; i < 4; i++) {
                                c = (char) ((c << 4) + hexCharToChar(readChar()));
                            }
                            break;
    
                        default:
                            reportError(String.format("Invalid escape character %s", toChar(c)));
                    }
                    break;
 
                case '"':
                    return new CBORTextString(s.toString());
                    
                default:
                    if (c < ' ') {
                        reportError(String.format("Unexpected control character: %s", toChar(c)));
                    }
            }
            s.append(c);
        }
    }
    
    private char hexCharToChar(char c) throws IOException {
        switch (c) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return (char) (c - '0');
    
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                return (char) (c - 'a' + 10);
    
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                return (char) (c - 'A' + 10);
        }
        reportError(String.format("Bad hex character: %s", toChar(c)));
        return 0; // For the compiler...
    }

    private char readChar() throws IOException {
        if (index >= json.length) {
            reportError("Unexpected EOF");
        }
        return json[index++];
    }

    private void scanNonSignficantData() throws IOException {
        while (index < json.length) {
            switch (nextChar()) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    readChar();
                    continue;

                 default:
                    return;
            }
        }
    }
}
