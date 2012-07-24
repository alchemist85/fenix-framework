package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;

import pt.ist.fenixframework.pstm.DomainMetaClass;
import pt.ist.fenixframework.pstm.NoDomainMetaObjects;

/**
 * A <code>PrivateConsistencyPredicate</code> is a
 * {@link DomainConsistencyPredicate} that represents a predicate method that is
 * private. It can neither override, nor be overridden by other consistency
 * predicates.
 * 
 * Therefore, during the initialization, the new
 * <code>PrivateConsistencyPredicate</code> is executed for all objects of the
 * declaring domain class and subclasses. Likewise, on deletion, all its
 * {@link DomainDependenceRecord}s are removed.
 * 
 * @author João Neves - JoaoRoxoNeves@ist.utl.pt
 **/
@NoDomainMetaObjects
public class PrivateConsistencyPredicate extends PrivateConsistencyPredicate_Base {

    public PrivateConsistencyPredicate(Method predicateMethod, DomainMetaClass metaClass) {
	super();
	setPredicate(predicateMethod);
	setDomainMetaClass(metaClass);
	System.out.println("[ConsistencyPredicates] Created a " + getClass().getSimpleName() + " for " + getPredicate());
    }

    @Override
    public boolean isPublic() {
	return false;
    }

    @Override
    public boolean isPrivate() {
	return true;
    }

    @Override
    public boolean isFinal() {
	return false;
    }

    /**
     * This method does nothing. A <code>PrivateConsistencyPredicate</code>
     * cannot override other predicates.
     */
    @Override
    public void initConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    /**
     * This method does nothing. A <code>PrivateConsistencyPredicate</code>
     * cannot override other predicates.
     */
    @Override
    public void updateConsistencyPredicateOverridden() {
	checkFrameworkNotInitialized();
    }

    /**
     * This method does nothing. A <code>PrivateConsistencyPredicate</code>
     * cannot be overridden by other predicates.
     */
    @Override
    public void checkOverridingMethods(DomainMetaClass metaClass) {
    }

    /**
     * Executes this consistency predicate for all objects of the given
     * {@link DomainMetaClass}, and all objects of subclasses. Because the
     * predicate is private, it cannot be overridden at any subclass.
     * 
     * @param metaClass
     *            the {@link DomainMetaClass} for which to execute this
     *            predicate.
     */
    @Override
    public void executeConsistencyPredicateForMetaClassAndSubclasses(DomainMetaClass metaClass) {
	executeConsistencyPredicateForExistingDomainObjects(metaClass);

	for (DomainMetaClass metaSubclass : metaClass.getDomainMetaSubclasses()) {
	    executeConsistencyPredicateForMetaClassAndSubclasses(metaSubclass);
	}
    }
}
