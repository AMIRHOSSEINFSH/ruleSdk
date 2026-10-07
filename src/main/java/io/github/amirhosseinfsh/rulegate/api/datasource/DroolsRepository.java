package io.github.amirhosseinfsh.rulegate.api.datasource;


import io.github.amirhosseinfsh.rulegate.api.dto.CreateRuleSetBaseDto;
import io.github.amirhosseinfsh.rulegate.api.dto.DeleteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.domain.dto.LocalRuleSetDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity;

import java.util.Set;

/**
 * an interface that have all action for manging Scenarios and their rules
 * that can have various implementation base on various context
 *
 * @param <T> extent CreateRuleSetBaseDto
 * @param <D> extend DeleteScenarioBaseDto
 */
public interface DroolsRepository<T extends CreateRuleSetBaseDto, D extends DeleteScenarioBaseDto> {

    /**
     * that save a RuleScenarioEntity and extra fields
     * Every rule must save under a scenario
     *
     * @param saveRuleSetContext must extend from CreateRuleSetBaseDto
     */
    void createRuleScenario(T saveRuleSetContext);

    /**
     * that delete base on scenarioName and extra filed on various extends of DeleteScenarioBaseDto
     *
     * @param deleteScenarioBaseDto must extend from DeleteScenarioBaseDto
     */
    void deleteScenario(D deleteScenarioBaseDto);

    /**
     * return all rule content base on scenarioName
     *
     * @param scenarioName that every rule must save under a scenario
     * @return set of rule content
     */
    Set<RuleItemEntity> getAllRuleContents(String scenarioName);

    /**
     * update one ruleContent in all scenarios base on ruleName
     *
     * @param ruleName    unique ruleName that save for all rules
     * @param ruleContent updated content of rule
     */
    void updateRuleContent(String ruleName, String ruleContent);

    /**
     * get set of all scenarioNames that have this ruleName
     *
     * @param ruleName unique name of rule
     * @return set of ScenarioName
     */
    Set<String> getAllScenariosByRuleName(String ruleName);

    /**
     * get list of all scenarioName
     *
     * @return set of scenarioName
     */
    Set<String> getAllScenarioName();

    /**
     * get content of a rule base on unique RuleName
     *
     * @param ruleName unique ruleName
     * @return content of rule
     */
    String getRuleContent(String ruleName);

    /**
     * return Scenario information as RuleScenarioEntity
     *
     * @param scenarioName scenario name should not be empty
     * @return LocalRuleSetDto that include scenario and ruleItems and kieContainer
     */
    LocalRuleSetDto getRuleEntitiesByScenario(String scenarioName);


    /// validations methods

    /**
     * validate DeleteRuleBaseDto that have necessary information
     *
     * @param deleteRuleBaseDto any object extend from DeleteRuleBaseDto
     * @return if true mean all required is fulfilled
     */
    boolean validateDeleteRuleDto(D deleteRuleBaseDto);

    /**
     * validate saveRuleBaseDto before save
     *
     * @param saveRuleSetContext any object that extend from CreateRuleSetBaseDto
     * @return true if all validations fulfills
     */
    boolean validateSaveRuleSetContext(T saveRuleSetContext);

    /**
     * check for existing scenario by name
     *
     * @param scenarioName should not be null
     * @return true if scenario by this name exist
     */
    boolean isScenarioExists(String scenarioName);

    /**
     * check for existing rule by its name
     *
     * @param ruleName should not be null
     * @return true if that rule exist
     */
    boolean isRuleExists(String ruleName);
}
