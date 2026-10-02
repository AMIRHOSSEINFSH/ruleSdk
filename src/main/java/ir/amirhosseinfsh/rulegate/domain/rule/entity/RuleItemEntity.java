package ir.amirhosseinfsh.rulegate.domain.rule.entity;

import ir.amirhosseinfsh.rulegate.domain.entity.BaseRuleEntity;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RuleItemEntity extends BaseRuleEntity {

    private Map<String,String> globals;

    public void setGlobals(Map<String,String> globals) {
        this.globals = globals;
    }

    public Map<String,String> getGlobals() {
        return globals;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        RuleItemEntity that = (RuleItemEntity) o;
        return getRuleName().equals(that.getRuleName()) && getPackageName().equals(that.getPackageName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRuleName(), getPackageName());
    }
}
