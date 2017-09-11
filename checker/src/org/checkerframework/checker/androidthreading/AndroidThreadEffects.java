package org.checkerframework.checker.androidthreading;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.GenericEffectLattice;

/*
 * See documentation on Thread annotations:
 *  https://developer.android.com/studio/write/annotations.html#thread-annotations
 * and on adding these annotations to the class path for a project:
 *  https://developer.android.com/studio/write/annotations.html#adding-library
 */
public final class AndroidThreadEffects implements GenericEffectLattice {

    private ArrayList<Class<? extends Annotation>> effects;
    public final Class<? extends Annotation> MainThread;
    public final Class<? extends Annotation> UiThread;
    public final Class<? extends Annotation> WorkerThread;
    public final Class<? extends Annotation> BinderThread;
    public final Class<? extends Annotation> AnyThread;

    public static AndroidThreadEffects getAndroidThreadEffects() throws ClassNotFoundException {
        return new AndroidThreadEffects();
    }

    @SuppressWarnings("unchecked")
    private AndroidThreadEffects() throws ClassNotFoundException {
        effects = new ArrayList<>();

        // To avoid having the Checker Framework depend on the Android
        // SDK, we retrieve these qualifiers by reflection.
        MainThread =
                (Class<? extends Annotation>)
                        Class.forName("android.support.annotation.MainThread");
        UiThread =
                (Class<? extends Annotation>) Class.forName("android.support.annotation.UiThread");
        WorkerThread =
                (Class<? extends Annotation>)
                        Class.forName("android.support.annotation.WorkerThread");
        BinderThread =
                (Class<? extends Annotation>)
                        Class.forName("android.support.annotation.BinderThread");
        AnyThread =
                (Class<? extends Annotation>) Class.forName("android.support.annotation.AnyThread");

        effects.add(MainThread);
        effects.add(UiThread);
        effects.add(WorkerThread);
        effects.add(BinderThread);
        effects.add(AnyThread);
    }

    /**
     * Method to check Less than equal to Effect
     *
     * @param left : Left Effect
     * @param right: Right Effect
     * @return boolean true : if bottom effect is left effect and is equal to NoIOEffect OR if top
     *     effect is right effect and is equal to IOEffect OR if left effect and right effect are
     *     the same
     *     <p>false : otherwise
     */
    @Override
    public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
        assert (left != null && right != null);

        return left.equals(right) || left.equals(AnyThread);
    }

    /**
     * Method to get minimum of (l, r)
     *
     * @param l : left effect
     * @param r : right effect
     * @return minimum(l,r)
     */
    @Deprecated
    @Override
    public Class<? extends Annotation> min(
            Class<? extends Annotation> l, Class<? extends Annotation> r) {
        if (LE(l, r)) {
            return l;
        } else {
            return r;
        }
    }

    /** Get the collection of valid effects. */
    @Override
    public ArrayList<Class<? extends Annotation>> getValidEffects() {
        return effects;
    }

    /**
     * Get the Top Most Effect of Lattice. For IO EFfect checker: Top Most Effect of Lattice:
     * IOEffect
     */
    @Deprecated
    @Override
    public Class<? extends Annotation> getTopMostEffectInLattice() {
        return null;
    }

    /**
     * Get the Bottom Most Effect of Lattice. For IO EFfect checker: Bottom Most Effect of Lattice:
     * NoIOEffect
     */
    @Override
    public Class<? extends Annotation> getBottomMostEffectInLattice() {
        return AnyThread;
    }
}
