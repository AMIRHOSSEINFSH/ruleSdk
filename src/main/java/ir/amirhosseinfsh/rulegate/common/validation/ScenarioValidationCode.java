package ir.amirhosseinfsh.rulegate.common.validation;

public enum ScenarioValidationCode implements DroolsValidationCode  {
  scenario_NOT_FOUND,
  scenario_MUST_NOT_EXISTS,
  scenario_MUST_EXISTS,
  KIECONTAINER_REQUIRED,
  SOMETHING_WENT_WRONG,
  RULE_ITEMS_REQUIRED,
  EXECUTION_ERROR;

  @Override
  public String getCode() {
    return this.name();
  }
}
