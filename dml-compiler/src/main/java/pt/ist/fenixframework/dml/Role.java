package pt.ist.fenixframework.dml;

import java.io.Serializable;

import org.json.simple.JSONObject;


public class Role implements Serializable{
    public static final int MULTIPLICITY_MANY = -1;

    private String name;
    private DomainEntity type;
    private int multiplicityLower = 0;
    private int multiplicityUpper = 1;
    private DomainRelation relation;
    private String indexProperty;
    private boolean ordered = false;

    private JSONObject meta;
    

    public Role(String name, DomainEntity type) {
        this.name = name;
        this.type = type;
    }

    public DomainRelation getRelation() {
        return relation;
    }

    public boolean isFirstRole() {
        return relation.getFirstRole() == this;
    }

    public Role getOtherRole() {
        return (isFirstRole() ? relation.getSecondRole() : relation.getFirstRole());
    }

    public void setRelation(DomainRelation relation) {
        this.relation = relation;
    }

    public String getName() {
        return name;
    }

    public DomainEntity getType() {
        return type;
    }

    public void setMultiplicity(int lower, int upper) {
        this.multiplicityLower = lower;
        this.multiplicityUpper = upper;
    }

    public int getMultiplicityLower() {
        return multiplicityLower;
    }

    public int getMultiplicityUpper() {
        return multiplicityUpper;
    }

    public void setIndexProperty(String propName) {
        this.indexProperty = propName;
    }

    public String getIndexProperty() {
        return indexProperty;
    }

    public void setOrdered(boolean ordered) {
        this.ordered = ordered;
    }

    public boolean getOrdered() {
        return ordered;
    }

    public boolean needsMultiplicityChecks() {
        return (multiplicityLower > 0)
            || ((multiplicityUpper > 1) 
                && (multiplicityUpper != MULTIPLICITY_MANY));
    }

    public JSONObject getMetadata() {
	return meta;
    }
    
    public void setMetadata(JSONObject meta) {
	this.meta = meta;
    }
}

