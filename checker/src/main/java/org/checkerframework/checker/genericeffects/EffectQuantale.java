package org.checkerframework.checker.genericeffects;

import java.util.ArrayList;

public abstract class EffectQuantale<X> {

    // Method to check Less than equal to Effect
    public boolean LE(X left, X right) {
        return LUB(left,right).equals(right);
    }

    public abstract X LUB(X l, X r);

    public abstract X seq(X l, X r);

    // Get the collection of valid effects.
    public abstract ArrayList<X> getValidEffects();

    // Get the Bottom Most Effect of Lattice
    public abstract X unit();

    public abstract X iter(X x);

    public abstract X residual(X sofar, X target);
}
