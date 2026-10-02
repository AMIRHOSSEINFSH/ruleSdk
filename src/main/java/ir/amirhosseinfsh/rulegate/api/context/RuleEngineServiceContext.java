package ir.amirhosseinfsh.rulegate.api.context;

import ir.amirhosseinfsh.rulegate.api.datasource.DroolsRepository;
import ir.amirhosseinfsh.rulegate.api.dto.CreateRuleSetBaseDto;
import ir.amirhosseinfsh.rulegate.api.dto.DeleteRuleBaseDto;
import ir.amirhosseinfsh.rulegate.api.dto.DeleteScenarioBaseDto;
import ir.amirhosseinfsh.rulegate.api.dto.ExecuteScenarioBaseDto;
import ir.amirhosseinfsh.rulegate.domain.rule.dto.RuleDto;
import ir.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;
import ir.amirhosseinfsh.rulegate.domain.rule.viewDto.RuleViewDto;

import java.util.List;
import java.util.Set;

/**
 * all drools operation include creating kieContainer and remove it and crud for scenarioRule
 * and execute rules again facts do in this service
 *
 * @param <T> must extend from CreateRuleSetBaseDto that include RuleScenarioEntity
 * @param <D> must extend from DeleteScenarioBaseDto that include scenarioName
 */
public interface RuleEngineServiceContext<T extends CreateRuleSetBaseDto, D extends DeleteScenarioBaseDto> {

    /**
     * return DroolsRepository object
     *
     * @return DroolsRepository object that better implement for every context
     */
    DroolsRepository<T, D> getDataSourceRepository();

    /**
     * create kieContainer and RuleScenarioEntity from ruleDto
     * and save it by {@link DroolsRepository#createRuleScenario(T saveRuleSetContext)}
     *
     * @param ruleDto dto that include scenarioName and list of ruleContent
     */
    void createScenario(RuleDto ruleDto);

    /**
     * delete rule base on specific scenario and ruleName
     * first get scenario base on scenarioName {@link DroolsRepository#getRuleEntitiesByScenario(String scenarioName)}
     * after remove rule create another by {@link DroolsRepository#createRuleScenario(T ruleSetDto)}
     *
     * @param deleteRuleBaseDto extend DeleteScenarioBaseDto that add ruleName to it
     */
    void deleteRule(DeleteRuleBaseDto deleteRuleBaseDto);

    /**
     * first check scenario exist by {@link  DroolsRepository#isScenarioExists}
     * if exist delete scenario and its rules from Context by {@link  DroolsRepository#deleteScenario}
     *
     * @param deleteScenarioBaseDto should not be empty and include must be valid
     */
    void deleteScenario(DeleteScenarioBaseDto deleteScenarioBaseDto);

    /**
     * base on scenario find created kieContainer and execute rules again facts
     *
     * @param executeScenarioDto include scenario name and fact and globals object and can have agendaGroup
     */
    void execute(ExecuteScenarioBaseDto executeScenarioDto);


    /**
     * get list of all scenarios  by {@link  DroolsRepository#getAllScenarioName}
     *
     * @return set of scenario name
     */
    Set<String> getAllScenarios();

    /**
     * get all rules by {@link DroolsRepository#getRuleEntitiesByScenario}
     *
     * @param scenarioName should not be empty
     * @return an object that include list of RuleItemViewDto
     */
    RuleViewDto getRuleSetByScenarioName(String scenarioName);

    /**
     * get rule content by ruleName {@link DroolsRepository#getRuleContent}
     *
     * @param ruleName should not be null
     * @return rule content
     */
    String getRuleContext(String ruleName);

    /**
     * extract rule name from rule content and search rule by ruleName
     * if exist update ruleContent by {@link  DroolsRepository#updateRuleContent}
     *
     * @param ruleContent should not be empty
     */
    void updateRule(String ruleContent);

    void addRuleToScenario(RuleDto ruleDto);

    boolean isScenarioExists(String scenarioName);

    void validateRuleSet(List<String> ruleContentList);

    RuleViewDto convertToRuleViewDto(RuleScenarioEntity rulescenarioEntity);
}
