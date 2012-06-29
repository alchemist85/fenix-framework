package pt.ist.fenixframework.pstm.consistencyPredicates;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import jvstm.cps.Depended;
import jvstm.cps.DependenceRecord;
import pt.ist.fenixframework.pstm.AbstractDomainObject;
import pt.ist.fenixframework.pstm.DomainMetaObject;
import pt.ist.fenixframework.pstm.NoDomainMetaObjects;

/**
 * A <code>DomainDependenceRecord</code> represents the result of the execution
 * of a consistency predicate method for a dependent domain object. The
 * <code>DomainDependenceRecord</code> has a set of depended
 * {@link DomainMetaObject}s, that represent the other domain objects that were
 * read to execute the consistency predicate. If any of these objects is
 * modified, the consistency predicate must be reexecuted.<br>
 * <br>
 * A <code>DomainDependenceRecord</code> will never register the dependent
 * object as a depended object. After executing a consistency predicate, a
 * <code>DomainDependenceRecord</code> will not even be created if:
 * <ul>
 * <li>the object does not depend on other objects, and</li>
 * <li>the object is consistent</li>
 * </ul>
 * 
 * @author João Neves - JoaoRoxoNeves@ist.utl.pt
 **/
@NoDomainMetaObjects
public class DomainDependenceRecord extends DomainDependenceRecord_Base implements
	DependenceRecord<DomainMetaObject> {

    public DomainDependenceRecord(Object dependent, DomainConsistencyPredicate predicate, Set<Depended> depended,
	    boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getDomainMetaObject());
	setDomainConsistencyPredicate(predicate);
	for (Depended<DomainDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((DomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    public DomainDependenceRecord(Object dependent, Method predicate, Set<Depended> depended, boolean consistent) {
	super();
	setDependentDomainMetaObject(((AbstractDomainObject) dependent).getDomainMetaObject());
	setPredicate(predicate);
	for (Depended<DomainDependenceRecord> dependedMetaObject : depended) {
	    addDependedDomainMetaObjects((DomainMetaObject) dependedMetaObject);
	}
	setConsistent(consistent);
    }

    /**
     * Sets this <code>DomainDependenceRecord</code> as being consistent, or
     * inconsistent. Also updates the <code>inconsistentDependenceRecords</code>
     * relation to the {@link DomainConsistencyPredicate}.<br>
     * This method should only be invoked after the execution of a consistency
     * predicate.
     */
    @Override
    public void setConsistent(Boolean consistent) {
	if (consistent) {
	    if ((isConsistent() != null) && !isConsistent()) {
		// Setting to consistent after being inconsistent
		getDomainConsistencyPredicate().removeInconsistentDependenceRecords(this);
	    }
	} else {
	    if ((isConsistent() == null) || isConsistent()) {
		// Setting to inconsistent after being consistent, or for the first time
		getDomainConsistencyPredicate().addInconsistentDependenceRecords(this);
	    }
	}
	super.setConsistent(consistent);
    }

    public Boolean isConsistent() {
	return getConsistent();
    }

    /**
     * Deletes this <code>DomainDependenceRecord</code> after disconnecting it
     * from all relations.<br>
     * A <code>DomainDependenceRecord</code> can be deleted in two cases:
     * <ul>
     * <li>When the dependent object is being deleted. In this case, its
     * {@link DomainMetaObject} will also be deleted, and will therefore invoke
     * this method.</li>
     * <li>The {@link DomainConsistencyPredicate} was removed from the code. In
     * this case, we only remove the link to all the meta objects, which are not
     * deleted.</li>
     * </ul>
     **/
    public void delete() {
	for (DomainMetaObject dependedMetaObject : getDependedDomainMetaObjects()) {
	    removeDependedDomainMetaObjects(dependedMetaObject);
	}
	if (!isConsistent()) {
	    getDomainConsistencyPredicate().removeInconsistentDependenceRecords(this);
	}
	removeDomainConsistencyPredicate();
	removeDependentDomainMetaObject();

	//Deletes THIS DependenceRecord, which is also a Fenix-Framework DomainObject
	deleteDomainObject();
    }

    // DependenceRecord interface implemented below:
    @Override
    public void addDepended(DomainMetaObject dependedMetaObject) {
	addDependedDomainMetaObjects(dependedMetaObject);
    }

    @Override
    public Iterator<DomainMetaObject> getDepended() {
	return getDependedDomainMetaObjectsIterator();
    }

    @Override
    public Object getDependent() {
	return getDependentDomainMetaObject().getDomainObject();
    }

    public <DomainClass extends AbstractDomainObject> DomainClass getDependentDomainObject() {
	return (DomainClass) getDependent();
    }

    public void setPredicate(Method predicateMethod) {
	setDomainConsistencyPredicate(DomainConsistencyPredicate.readDomainConsistencyPredicate(predicateMethod));
    }

    @Override
    public Method getPredicate() {
	return getDomainConsistencyPredicate().getPredicate();
    }
}
