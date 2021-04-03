package org.checkerframework.checker.upload;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import org.checkerframework.checker.genericeffects.GenericEffectLattice;
import org.checkerframework.checker.upload.qual.WriteOnDisk;
import org.checkerframework.checker.upload.qual.Noop;
import org.checkerframework.checker.upload.qual.Send;
import org.checkerframework.checker.upload.qual.Flush;

public final class UploadEffects implements GenericEffectLattice {

	private ArrayList<Class<? extends Annotation>> effects = new ArrayList<>();
	//public final Class<? extends Annotation> WriteOnDisk;
	//public final Class<? extends Annotation> Noop;	

	public static UploadEffects getUploadEffects() throws ClassNotFoundException{
		return new UploadEffects();
	}
	
	private UploadEffects() {
		//WriteOnDisk = getAnnotation("upload.support.annotation.WriteOnDisk");
		effects.add(WriteOnDisk.class);
		effects.add(Noop.class);
		effects.add(Send.class);
		effects.add(Flush.class);
	}
	
	/**
     	* Method to check Less than equal to Effect
     	*
     	* @param left : Left Effect
     	* @param right: Right Effect
     	* @return boolean true : if bottom effect is on the left and the top effect is on the right, or
     	*     if effects are equal
     	*     <p>false : otherwise
     	*     
     	*/
	@Override
	public boolean LE(Class<? extends Annotation> left, Class<? extends Annotation> right) {
		assert (left != null && right != null);
		
		if(right.equals(WriteOnDisk.class))
			return left.equals(WriteOnDisk.class)
				|| left.equals(Noop.class);
		else if (right.equals(Noop.class))
			return left.equals(Noop.class);

		return false;
	}

	/** Get the collection of valid effects. */
    	@Override
    	public ArrayList<Class<? extends Annotation>> getValidEffects() {
    	    return effects;
    	}

	/**
    	 * Get the Bottom Most Effect of Lattice. For IO EFfect checker: Bottom Most Effect of Lattice:
    	 * NoIOEffect
    	 */
    	@Override
    	public Class<? extends Annotation> unit() {
    	    return Noop.class;
    	}

    public Class<? extends Annotation> residual(Class<? extends Annotation> sofar, Class<? extends Annotation> target) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public Class<? extends Annotation> iter(Class<? extends Annotation> x) {
        // TODO
        throw new UnsupportedOperationException();
    }
}

