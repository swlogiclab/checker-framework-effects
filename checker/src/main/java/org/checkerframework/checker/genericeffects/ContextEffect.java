package org.checkerframework.checker.genericeffects;

import com.sun.source.tree.Tree;

import org.checkerframework.checker.genericeffects.qual.Impossible;

import java.lang.annotation.Annotation;
import java.util.AbstractMap;
import java.util.Deque;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the effect of all path executions from the start of a method
 * up to the current program point the checker is examining.
 */
public class ContextEffect<X> {

    // We abuse Map.Entry as a pair type that doesn't require an extra dependency
    private Deque<Map.Entry<X,Tree>> rep;
    private Deque<X> snapshots;
    private X context;
    private X contextSinceLastMark;
    private EffectQuantale<X> lat;

    //private Map<Class<? extends Exception>, Set<Map.Entry<X,Tree>>> excMap;

    // Suppress ErrorProne complaints about access speed; we need a stack impl that supports null.
    @SuppressWarnings("JdkObsolete")
    public ContextEffect(EffectQuantale<X> l) {
        rep = new LinkedList<>();
        snapshots = new LinkedList<>();
        lat = l;
        context = lat.unit();
        contextSinceLastMark = lat.unit();
        //excMap = new HashMap<>();
    }

    /**
     * Mark the context in places restoration will be required
     */
    public void mark() {
        rep.addFirst(null);
        snapshots.addFirst(context);
        snapshots.addFirst(contextSinceLastMark);
	// Context since last mark needs to now be unit
	contextSinceLastMark = lat.unit();
    }

    /**
     * Return the effects up to the last snapshot, in program-order
     * 
     * Suppress ErrorProne complaints about access speed; we need a stack impl that supports null.
     */
    @SuppressWarnings("JdkObsolete")
    public LinkedList<X> rewindToMark() {
        LinkedList<X> results = new LinkedList<>();
        while (rep.peekFirst() != null) {
            results.addFirst(rep.removeFirst().getKey());
        }
        rep.removeFirst(); // remove null
        contextSinceLastMark = snapshots.removeFirst();
        context = snapshots.removeFirst();
        return results;
    }

    /**
     * Add an effect sequenced after the current execution path(s) summarized by the context.
     * 
     * @param eff The effect sequenced
     * @param t The {@link Tree} instance for the ast leading to this effect
     * @return True when sequencing was valid, false when sequencing was undefined
     */
    public boolean pushEffect(X eff, Tree t) {
        rep.addFirst(new AbstractMap.SimpleEntry<X,Tree>(eff,t));
        // TODO: do legwork to verify this assertion is true (i.e., enforced by Java's compiler)
        assert context != null: "System assumes it is impossible to have code visited after (only) returns and/or throws";
        // TODO: Need to deal with case where seq is undefined, and recover. leave unmodified if seq fails, and return boolean to caller?
        X sq = lat.seq(context, eff);
        if (sq != null) {
            context = sq;
            contextSinceLastMark = lat.seq(contextSinceLastMark, eff);
            return true;
        } else {
            return false;
        }
    }

    public void markImpossible(Tree t) {
        rep.addFirst(new AbstractMap.SimpleEntry<X,Tree>(null,t));
        context = null;
        contextSinceLastMark = null;
    }

    ///**
    // * Stashes the current effect (since start of method) in the exception map
    // * 
    // * This needs to track the full effect from the start of method execution, since the throwing
    // * location could be deep in an AST and we won't know at this call site how many marks might
    // * be on the stack.
    // * 
    // * @param excType The class type of the exception being thrown
    // * @param t The tree of the throwing location (call or explicit throw)
    // * @return true if the tracking was successful (currently always)
    // */
    //public boolean trackExplicitThrow(Class<? extends Exception> excType, Tree t) {
    //    X path = currentPathEffect();
    //    // TODO: HIPRI(soundness): need to query *supertypes* of this exception type!
    //    var priors = excMap.get(excType);
    //    if (priors == null) {
    //        priors = new HashSet<Map.Entry<X,Tree>>();
    //    }
    //    priors.add(new AbstractMap.SimpleEntry<>(path, t));
    //    // This is an explicit throw, so this branch of computation should be marked impossible.
    //    markImpossible(t);
    //    return true;
    //}

    //public boolean unresolvedExceptions() {
    //    return !excMap.isEmpty();
    //}

    //// TODO: need way to handle implicit throws from calls, in which case there will also be an effect up to the throw for the call itself which won't be on the stack when this is called

    //public Set<Map.Entry<X,Tree>> catchException(Class<? extends Exception> excType) {
    //    // This needs to temporarily restore the full in-context effect as a join of all throwing paths,
    //    // but we need to be able to go back to the original. Since we also have a number of summary
    //    // pieces, those also need to be backed up and restored.

    //    // To reduce bugs, let's opt for a slightly less convenient API that will let us flag mismangement of exception tracking: it should trigger a runtime exception if this is called when the regular path is already frozen/backed-up.  This means we need a "finish that exception" call which restores the backed up version, and it needs to be cheap enough to be called after every catch block. This actually needs to also merge the exception(s)+catch path effects back into the regular path... which sounds like a question for the caller that knows what marks are in place... so maybe instead of really doing a state update, this should just extract + remove + return the relevant effects for this exception, and leave it to the caller to manage marks (e.g., setting a mark for the try body and swapping out above that mark). Similarly for tracking, we should actually just provide one throw tracking, and leave it to the caller to handle impossibility for calls vs throws.

    //    // Also need to ensures things work correctly if another exception is raised inside a catch block

    //    // Another alternative would be to keep a stack of try block throws ('try scope'). Then visitMethod and visitTry could push a new empty batch,
    //    // and catchException could look at only the top-most batch (for the corresponding try body).  Then visitTry
    //    // could merge any remaining exceptional paths into the next one up the stack, and any left at the end of visiting the method would be compared to the throwneffect clauses.

    //    // Breaks could be handled similarly, with a stack of break scopes... which would require another summary (effect since last breakscope, like the also-needed effect since last try scope), so when a break scope was exited the visitor could collect and transform/merge as necessary, e.g. setting a scope for a loop construct, then adding that as a possible loop tail effect.

    //    // The rewind ops would need to return a full batch of these three pieces (i.e., a ControlEffect), which the visitor
    //    // could then use directly for merges and such.

    //}

    public void finishException() {
        // TODO: restore cached non-exceptional path info

    }

    public X currentPathEffect() {
        return context;
    }
    public boolean currentlyImpossible() {
        return contextSinceLastMark == null;
    }

    public X latestEffect() {
        return rep.peekFirst().getKey();
    }
    public X squashMark(Tree t) {
        X squashed = contextSinceLastMark;
	System.err.println("saved contextSinceLastMark: "+squashed);
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
