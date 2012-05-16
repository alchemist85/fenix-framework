package pt.ist.fenixframework.pstm;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;

import pt.ist.fenixframework.DomainObject;

public abstract class Transaction extends jvstm.Transaction {
    public final static TransactionStatistics STATISTICS = new TransactionStatistics();
    private final static FenixCache cache = new FenixCache();

    static {
	try {
	    Class.forName("pt.ist.fenixframework.FenixFrameworkInitializer");
	} catch (final ClassNotFoundException e) {
	    System.out.println("No application specific initialization method.");
	}

	DomainClassInfo.initializeClassInfos(0);

	jvstm.Transaction.setTransactionFactory(new jvstm.TransactionFactory() {
	    @Override
	    public jvstm.Transaction makeTopLevelTransaction(jvstm.ActiveTransactionsRecord record) {
		return new TopLevelTransaction(record);
	    }

	    @Override
	    public jvstm.Transaction makeReadOnlyTopLevelTransaction(jvstm.ActiveTransactionsRecord record) {
		return new ReadOnlyTopLevelTransaction(record);
	    }
	});

	// initialize transaction system
	int maxTx = TransactionChangeLogs.initializeTransactionSystem();
	if (maxTx >= 0) {
	    System.out.println("Setting the last committed TX number to " + maxTx);
	    mostRecentRecord = new jvstm.ActiveTransactionsRecord(maxTx, null);
	} else {
	    throw new Error("Couldn't determine the last transaction number");
	}
    }

    static jvstm.ActiveTransactionsRecord getActiveRecordForNewTransaction() {
	return mostRecentRecord.getRecordForNewTransaction();
    }

    private Transaction() {
	// this is never to be used!!!
	super(0);
    }

    public static void beginTransaction() {
	if (jvstm.Transaction.current() != null) {
	    jvstm.Transaction.commit();
	}
	Transaction.begin();
    }

    private static final ThreadLocal<Boolean> DEFAULT_READ_ONLY = new ThreadLocal<Boolean>() {
	@Override
	protected Boolean initialValue() {
	    return Boolean.FALSE;
	}
    };

    public static boolean getDefaultReadOnly() {
	return DEFAULT_READ_ONLY.get().booleanValue();
    }

    public static void setDefaultReadOnly(boolean readOnly) {
	DEFAULT_READ_ONLY.set(readOnly ? Boolean.TRUE : Boolean.FALSE);
    }

    public static jvstm.Transaction begin() {
	return Transaction.begin(getDefaultReadOnly());
    }

    public static jvstm.Transaction begin(boolean readOnly) {
	return jvstm.Transaction.begin(readOnly);
    }

    // This method is here just as a quick solution for providing a
    // way to create read-only transactions that do not open a
    // connection to the database, but this should be done in a better
    // way, probably by changing the API provided by the JVSTM, so
    // that the call to the Transaction.begin(...) method may receive
    // an argument specifying the kind of transaction that we want.
    public static jvstm.Transaction beginReadOnlyPossiblyInThePast(long maxMillisInThePast) {
	// use normal ReadOnlyTopLevelTransaction if no dbConnection was made
	// recently
	if (!TopLevelTransaction.lastDbConnectionWithin(maxMillisInThePast)) {
	    return begin(true);
	}

	jvstm.Transaction parent = current.get();
	if (parent != null) {
	    throw new Error("This kind of transactions cannot be nested");
	}

	jvstm.ActiveTransactionsRecord activeRecord = mostRecentRecord.getRecordForNewTransaction();
	jvstm.Transaction tx = new ReadOnlyTopLevelTransactionPossiblyInThePast(activeRecord);
	tx.start();
	return tx;
    }

    public static void forceFinish() {
	if (current() != null) {
	    try {
		commit();
	    } catch (Throwable t) {
		System.out
			.println("Aborting from Transaction.forceFinish(). If being called from CloseTransactionFilter it will leave an open transaction.");
		abort();
	    }
	}
    }

    public static void abort() {
	STATISTICS.incAborts();

	jvstm.Transaction.abort();
    }

    public static FenixTransaction currentFenixTransaction() {
	return (FenixTransaction) current();
    }

    protected static DBChanges currentDBChanges() {
	return currentFenixTransaction().getDBChanges();
    }

    public static DomainObject readDomainObject(String classname, Integer oid) {
	return (oid == null) ? null : currentFenixTransaction().readDomainObject(classname, oid);
    }

    @Deprecated
    public static <T extends DomainObject> T getObjectForOID(long oid) {
	return AbstractDomainObject.<T> fromOID(oid);
    }

    public static void logAttrChange(DomainObject obj, String attrName) {
	currentDBChanges().logAttrChange(obj, attrName);
    }

    public static void storeNewObject(DomainObject obj) {
	currentDBChanges().storeNewObject(obj);
	((jvstm.cps.ConsistentTransaction) current()).registerNewObject(obj);
    }

    public static void storeObject(DomainObject obj, String attrName) {
	currentDBChanges().storeObject(obj, attrName);
    }

    public static void deleteObject(Object obj) {
	currentDBChanges().deleteObject(obj);
    }

    public static void addRelationTuple(String relation, DomainObject obj1, String colNameOnObj1, DomainObject obj2,
	    String colNameOnObj2) {
	currentDBChanges().addRelationTuple(relation, obj1, colNameOnObj1, obj2, colNameOnObj2);
    }

    public static void removeRelationTuple(String relation, DomainObject obj1, String colNameOnObj1, DomainObject obj2,
	    String colNameOnObj2) {
	currentDBChanges().removeRelationTuple(relation, obj1, colNameOnObj1, obj2, colNameOnObj2);
    }

    public static PersistenceBroker getOJBBroker() {
	return currentFenixTransaction().getOJBBroker();
    }

    // This method is temporary. It will be used only to remove the dependencies
    // on OJB from the remaining code. After that, it should either be removed
    // or replaced by something else
    public static java.sql.Connection getCurrentJdbcConnection() {
	try {
	    return getOJBBroker().serviceConnectionManager().getConnection();
	} catch (org.apache.ojb.broker.accesslayer.LookupException le) {
	    throw new Error("Couldn't find a JDBC connection");
	}
    }

    // This method is temporary. It will be used only to remove the dependencies
    // on OJB from the remaining code. After that, it should either be removed
    // or replaced by something else
    public static java.sql.Connection getNewJdbcConnection() {
	try {
	    return PersistenceBrokerFactory.defaultPersistenceBroker().serviceConnectionManager().getConnection();
	} catch (org.apache.ojb.broker.accesslayer.LookupException le) {
	    throw new Error("Couldn't find a JDBC connection");
	}
    }

    public static FenixCache getCache() {
	return cache;
    }

    public static void withTransaction(jvstm.TransactionalCommand command) {
	withTransaction(false, command);
    }

    public static void withTransaction(boolean readOnly, jvstm.TransactionalCommand command) {
	while (true) {
	    Transaction.begin(readOnly);
	    boolean txFinished = false;
	    try {
		command.doIt();
		Transaction.commit();
		txFinished = true;
		return;
	    } catch (jvstm.CommitException ce) {
		System.out.println("Restarting TX because of a conflict");
		Transaction.abort();
		txFinished = true;
	    } catch (AbstractDomainObject.UnableToDetermineIdException unableToDetermineIdException) {
		System.out.println("Restaring TX: unable to determine id");
		Transaction.abort();
		txFinished = true;
	    } finally {
		if (!txFinished) {
		    Transaction.abort();
		}
	    }
	}
    }
}
