package org.webpki.cbor;

public class CBORFloat32Test {
    
    static int float32;
    static int float16;
    static int runs;
    
    static void convert (int i) {
        try {
            float d = Float.intBitsToFloat(i);
            CBORDouble cbor = new CBORDouble(d);
            switch (cbor.headerTag) {
                case CBORObject.MT_FLOAT16:
                    float16++;
                    if (Double.isNaN(d)) break;
                    if (Double.isInfinite(d)) break;
                    break;
                case CBORObject.MT_FLOAT32:
                    float32++;
                    break;
                default:
                    throw new RuntimeException("BUG");
            }
            float v = (float)CBORObject.decode(cbor.encode()).getDouble();
            if ((++runs % 1000000) == 0) {
                System.out.println("V=" + d + " 16=" + float16 + " 32=" + float32);
            }
        } catch (Exception e) {
            System.out.println("**********=" + Long.toUnsignedString(i, 16));
            System.exit(3);
        }
    }
    
    public static void main(String[] argv)  {
        int f = 0;
        while (f < (1 << CBORObject.FLOAT32_FRACTION_SIZE)) {
            int e = 0;
            while (e < (1 << CBORObject.FLOAT32_EXPONENT_SIZE)) {
                convert((e << CBORObject.FLOAT32_FRACTION_SIZE) + f);
                e++;
            }
            f++;
        }
        System.out.println("Runs=" + Long.toString(runs));
    }
}