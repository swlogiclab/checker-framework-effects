package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.genericeffects.qual.Impossible;

import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the effect of all path executions from the start of a method
 * up to the current program point the checker is examining.
 */
public class ContextEffect {

    // We abuse Map.Entry as a pair type that doesn't require an extra dependency
    private Deque<Map.Entry<Class<? extends Annotation>,Tree>> rep;
    private Deque<Class<? extends Annotation>> snapshots;
    private Class<? extends Annotation> context;
    private Class<? extends Annotation> contextSinceLastMark;
    private GenericEffectLattice lat;

    public ContextEffect(GenericEffectLattice l) {
        rep = new LinkedList<>();
        snapshots = new LinkedList<>();
        lat = l;
        context = lat.unit();
        contextSinceLastMark = lat.unit();
    }

    /**
     * Mark the context in places restoration will be required
     */
    public void mark() {
        rep.addFirst(null);
        snapshots.addFirst(context);
        snapshots.addFirst(contextSinceLastMark);
    }

    /**
     * Return the effects up to the last snapshot, in program-order
     */
    public LinkedList<Class<? extends Annotation>> rewindToMark() {
        LinkedList<Class<? extends Annotation>> results = new LinkedList<>();
        while (rep.peekFirst() != null) {
            results.addFirst(rep.removeFirst().getKey());
        }
        rep.removeFirst(); // remove null
        contextSinceLastMark = snapshots.removeFirst();
        context = snapshots.removeFirst();
        return results;
    }

    /**
     * 
     * @param eff
     * @param t
     * @return True when sequencing was valid, false when sequencing was undefined
     */
    public boolean pushEffect(Class<? extends Annotation> eff, Tree t) {
        rep.addFirst(new AbstractMap.SimpleEntry<Class<? extends Annotation>,Tree>(eff,t));
        // TODO: do legwork to verify this assertion is true (i.e., enforced by Java's compiler)
        assert context != Impossible.class: "System assumes it is impossible to have code visited after (only) returns and/or throws";
        // TODO: Need to deal with case where seq is undefined, and recover. leave unmodified if seq fails, and return boolean to caller?
        var sq = lat.seq(context, eff);
        if (sq != null) {
            context = sq;
            contextSinceLastMark = lat.seq(contextSinceLastMark, eff);
            return true;
        } else {
            return false;
        }
    }

    public boolean markImpossible(Tree t) {
        rep.addFirst(new AbstractMap.SimpleEntry<Class<? extends Annotation>,Tree>(Impossible.class,t));
        context = Impossible.class;
        contextSinceLastMark = Impossible.class;
    }

    public Class<? extends Annotation> currentPathEffect() {
        return context;
    }
    public boolean currentlyImpossible() {
        return contextSinceLastMark == Impossible.class;
    }

    public Class<? extends Annotation> latestEffect() {
        return rep.peekFirst().getKey();
    }
    public Class<? extends Annotation> squashMark(Tree t) {
        Class<? extends Annotation> squashed = contextSinceLastMark;
        while (rep.peekFirst() != null) {
            rep.removeFirst();
        }
        rep.removeFirst(); // remove null
        contextSinceLastMark = snapshots.removeFirst();
        context = snapshots.removeFirst();
        pushEffect(squashed, t);
        return squashed;
    }

    //public List<Tree> currentTreePath() {
    //    LinkedList<Tree> results = new LinkedList<>();
    //    // infinite loop
    //    while (rep.peekFirst() != null) {
    //        results.addFirst(rep.peekFirst().getValue());
    //    }
    //    return results;
    //}

}
