import org.checkerframework.checker.genericeffects.qual.DecimalOverflow;
import org.checkerframework.checker.genericeffects.qual.DecimalPrecisionLoss;
import org.checkerframework.checker.genericeffects.qual.IntegerOverflow;
import org.checkerframework.checker.genericeffects.qual.IntegerPrecisionLoss;

public class BasicEffect {

    byte a = 12;
    short b = 123;
    int c = 1234;
    long d = 12345l;
    float e = 12345.67f;
    double f = 12345.67;

    @IntegerOverflow
    public void checkIntegerOverflow() {
        //:: warning: (cast.redundant)
        byte a1 = (byte) a;
        //okay
        short a2 = (short) a;
        //okay
        int a3 = (int) a;
        //okay
        long a4 = (long) a;

        //okay
        byte b1 = (byte) b;
        //:: warning: (cast.redundant)
        short b2 = (short) b;
        //okay
        int b3 = (int) b;
        //okay
        long b4 = (long) b;

        //okay
        byte c1 = (byte) c;
        //okay
        short c2 = (short) c;
        //:: warning: (cast.redundant)
        int c3 = (int) c;
        //okay
        long c4 = (long) c;

        //okay
        byte d1 = (byte) d;
        //okay
        short d2 = (short) d;
        //okay
        int d3 = (int) d;
        //:: warning: (cast.redundant)
        long d4 = (long) d;
    }

    @IntegerPrecisionLoss
    public void checkIntegerPrecisionLoss() {
        //okay
        float a1 = (float) a;
        //okay
        double a2 = (double) a;

        //okay
        float b1 = (float) b;
        //okay
        double b2 = (double) b;

        //okay
        float c1 = (float) c;
        //okay
        double c2 = (double) c;

        //okay
        float d1 = (float) d;
        //okay
        double d2 = (double) d;
    }

    @DecimalOverflow
    public void checkDecimalOverflow() {
        //okay
        byte e1 = (byte) e;
        //okay
        short e2 = (short) e;
        //okay
        int e3 = (int) e;
        //okay
        long e4 = (long) e;

        //okay
        byte f1 = (byte) f;
        //okay
        short f2 = (short) f;
        //okay
        int f3 = (int) f;
        //okay
        long f4 = (long) f;
    }

    @DecimalPrecisionLoss
    public void checkDecimalPrecisionLoss() {
        //:: warning: (cast.redundant)
        float e1 = (float) e;
        //okay
        double e2 = (double) e;

        //okay
        float f1 = (float) f;
        //:: warning: (cast.redundant)
        double f2 = (double) f;
    }
}
