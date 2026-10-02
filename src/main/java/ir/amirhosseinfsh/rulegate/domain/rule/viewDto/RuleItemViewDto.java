package ir.amirhosseinfsh.rulegate.domain.rule.viewDto;

import java.util.Map;

public class RuleItemViewDto {

    String ruleId;
    String ruleContent;
    private String packageName;
    private Map<String,String> globals;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Map<String, String> getGlobals() {
        return globals;
    }

    public void setGlobals(Map<String, String> globals) {
        this.globals = globals;
    }

    public RuleItemViewDto() {
    }

    public RuleItemViewDto(String ruleId, String ruleContent) {
        this.ruleId = ruleId;
        this.ruleContent = ruleContent;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleContent() {
        return ruleContent;
    }

    public void setRuleContent(String ruleContent) {
        this.ruleContent = ruleContent;
    }
}
