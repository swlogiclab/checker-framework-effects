public class BasicCast {

  byte a = 12;
  short b = 123;
  int c = 1234;
  long d = 12345l;
  float e = 12345.67f;
  double f = 12345.67;

  public void checkIntegerOverflow() {
    // :: warning: (cast.redundant)
    byte a1 = (byte) a;
    // okay
    short a2 = (short) a;
    // okay
    int a3 = (int) a;
    // okay
    long a4 = (long) a;

    // :: error: (operation.invalid)
    byte b1 = (byte) b;
    // :: warning: (cast.redundant)
    short b2 = (short) b;
    // okay
    int b3 = (int) b;
    // okay
    long b4 = (long) b;

    // :: error: (operation.invalid)
    byte c1 = (byte) c;
    // :: error: (operation.invalid)
    short c2 = (short) c;
    // :: warning: (cast.redundant)
    int c3 = (int) c;
    // okay
    long c4 = (long) c;

    // :: error: (operation.invalid)
    byte d1 = (byte) d;
    // :: error: (operation.invalid)
    short d2 = (short) d;
    // :: error: (operation.invalid)
    int d3 = (int) d;
    // :: warning: (cast.redundant)
    long d4 = (long) d;
  }

  public void checkIntegerPrecisionLoss() {
    // okay
    float a1 = (float) a;
    // okay
    double a2 = (double) a;

    // okay
    float b1 = (float) b;
    // okay
    double b2 = (double) b;

    // :: error: (operation.invalid)
    float c1 = (float) c;
    // okay
    double c2 = (double) c;

    // :: error: (operation.invalid)
    float d1 = (float) d;
    // :: error: (operation.invalid)
    double d2 = (double) d;
  }

  public void checkDecimalOverflow() {
    // :: error: (operation.invalid)
    byte e1 = (byte) e;
    // :: error: (operation.invalid)
    short e2 = (short) e;
    // :: error: (operation.invalid)
    int e3 = (int) e;
    // :: error: (operation.invalid)
    long e4 = (long) e;

    // :: error: (operation.invalid)
    byte f1 = (byte) f;
    // :: error: (operation.invalid)
    short f2 = (short) f;
    // :: error: (operation.invalid)
    int f3 = (int) f;
    // :: error: (operation.invalid)
    long f4 = (long) f;
  }

  public void checkDecimalPrecisionLoss() {
    // :: warning: (cast.redundant)
    float e1 = (float) e;
    // okay
    double e2 = (double) e;

    // :: error: (operation.invalid)
    float f1 = (float) f;
    // :: warning: (cast.redundant)
    double f2 = (double) f;
  }
}
