import org.checkerframework.checker.critical.qual.*;
import org.checkerframework.checker.genericeffects.qual.ThrownEffect;

public class PseudoHibernate {

    public class TxException extends Exception {}

	public interface Transaction {
	    @Locking
	    @ThrownEffect(exception=TxException.class, behavior=Basic.class)
	    public void begin();

	    @Unlocking
	    @ThrownEffect(exception=TxException.class, behavior=Basic.class)
	    public void commit();

	    @Unlocking
	    @ThrownEffect(exception=TxException.class, behavior=Unlocking.class)
	    public void rollback();
	    
	}

    public interface Session {
      @Locking
      @ThrownEffect(exception=TxException.class, behavior=Basic.class)
      public Transaction beginTransaction() throws TxException;
      @Entrant
      public void close();
      @Critical
      @ThrownEffect(exception=TxException.class, behavior=Critical.class)
      public Query createQuery(String s) throws TxException;
    }
    public abstract class SessionFactory {
        public abstract Session openSession();
    }
    public interface Query {
      public int executeUpdate() throws TxException;
    }

    /* Example template from Hibernate docs: https://docs.jboss.org/hibernate/orm/3.2/api/org/hibernate/Session.html
     * Adapted to split the data dependency from the control dependency.
     */
    @Entrant
    @ThrownEffect(exception = TxException.class, behavior=Entrant.class)
    public void docExample(SessionFactory factory) throws TxException {
        Session sess = factory.openSession();
        Transaction tx = null;;
        try {
            tx = sess.beginTransaction();
        } catch (TxException e) {
            sess.close();
	    throw e;
        }
        try {
            doWork(sess);
            tx.commit();
        } catch (TxException e) {
            tx.rollback();
            throw e;
        } finally {
            sess.close();
        }
    }

    @Critical
    @ThrownEffect(exception=TxException.class, behavior=Critical.class)
    public void doWork(Session sess) throws TxException {
        System.out.println("Ready to work...");
	// :: error: (undefined.residual)
	Transaction tx = sess.beginTransaction();
	Query q = sess.createQuery("...");
	q.executeUpdate();
        System.out.println("Working...");
    }
}
