public class LiteralCast {

    public void literals() {
        // okay
        byte a = (byte) -128;
        // okay
        byte b = (byte) 127;
        // :: error: (cast.invalid)
        byte c = (byte) -129;
        // :: error: (cast.invalid)
        byte d = (byte) 128;
        // okay
        short e = (short) -32768;
        // okay
        short f = (short) 32767;
        // :: error: (cast.invalid)
        short g = (short) -32769;
        // :: error: (cast.invalid)
        short h = (short) 32768;

        // okay
        byte i = (byte) -128L;
        // okay
        byte j = (byte) 127L;
        // :: error: (cast.invalid)
        byte k = (byte) -129L;
        // :: error: (cast.invalid)
        byte l = (byte) 128L;
        // okay
        short m = (short) -32768L;
        // okay
        short n = (short) 32767L;
        // :: error: (cast.invalid)
        short o = (short) -32769L;
        // :: error: (cast.invalid)
        short p = (short) 32768L;
        // okay
        int q = (int) -2147483648L;
        // okay
        int r = (int) 2147483647L;
        // :: error: (cast.invalid)
        int s = (int) -2147483649L;
        // :: error: (cast.invalid)
        int t = (int) 2147483648L;
    }
}
