package ir.amirhosseinfsh.rulegate.domain.impl;

import ir.amirhosseinfsh.rulegate.api.datasource.DroolsRepository;
import ir.amirhosseinfsh.rulegate.api.dto.DeleteScenarioBaseDto;
import ir.amirhosseinfsh.rulegate.common.exception.DroolsException;
import ir.amirhosseinfsh.rulegate.common.exception.RuleException;
import ir.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import ir.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import ir.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import ir.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import ir.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;
import ir.amirhosseinfsh.rulegate.domain.dto.LocalRuleSetDto;
import ir.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity;
import ir.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class LocalDroolsRepositoryImpl implements DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> {

    private static final Logger log = LoggerFactory.getLogger(LocalDroolsRepositoryImpl.class);
    private final Map<String, KieContainer> containers = new HashMap<>();
    private final Map<String, Set<RuleItemEntity>> containerRules = new HashMap<>();
    private final DroolsHelper droolsHelper;

    public LocalDroolsRepositoryImpl(DroolsHelper droolsHelper) {
        this.droolsHelper = droolsHelper;
    }

    @Override
    public void createRuleScenario(LocalRuleSetDto saveRuleSetContext) {
        saveRuleSetContext.validate();
        RuleScenarioEntity rulescenarioEntity = saveRuleSetContext.getRulescenarioEntity();
        KieContainer kieContainer = saveRuleSetContext.getKieContainer();
        KieContainer previous = containers.put(rulescenarioEntity.getScenario(), kieContainer);
        containerRules.put(rulescenarioEntity.getScenario(), rulescenarioEntity.getRuleItems());
        disposeContainer(previous);
    }

    @Override
    public void deleteScenario(DeleteScenarioBaseDto deletescenarioBaseDto) {
        deletescenarioBaseDto.validate();
        String scenarioName = deletescenarioBaseDto.getScenarioName();
        disposeContainer(containers.remove(scenarioName));
        containerRules.remove(scenarioName);
        log.info("scenario {} deleted", scenarioName);
    }

    @Override
    public Set<RuleItemEntity> getAllRuleContents(String scenarioName) {
        if (!containerRules.containsKey(scenarioName))
            throw new ScenarioException("no scenario with " + scenarioName, ScenarioValidationCode.scenario_NOT_FOUND);
        return new HashSet<>(containerRules.get(scenarioName));

    }

    @Override
    public void updateRuleContent(String ruleName, String ruleContent) {
        Set<String> affectedScenarios = getAllScenariosByRuleName(ruleName);
        Map<String, Set<RuleItemEntity>> replacements = new HashMap<>();
        Map<String, KieContainer> builtContainers = new HashMap<>();
        try {
            for (String scenario : affectedScenarios) {
                Set<RuleItemEntity> updatedRules = new HashSet<>();
                for (RuleItemEntity current : containerRules.get(scenario)) {
                    RuleItemEntity copy = copyRule(current);
                    if (ruleName.equals(copy.getRuleName())) {
                        copy.setRuleContent(ruleContent);
                    }
                    updatedRules.add(copy);
                }
                replacements.put(scenario, updatedRules);
                builtContainers.put(scenario, droolsHelper.buildKieContainer(updatedRules));
            }
        } catch (RuntimeException failure) {
            builtContainers.values().forEach(LocalDroolsRepositoryImpl::disposeContainer);
            throw failure;
        }
        for (String scenario : affectedScenarios) {
            KieContainer previous = containers.put(scenario, builtContainers.get(scenario));
            containerRules.put(scenario, replacements.get(scenario));
            disposeContainer(previous);
        }
    }

    private static void disposeContainer(KieContainer container) {
        if (container != null) {
            container.dispose();
            KieServices.Factory.get().getRepository().removeKieModule(container.getReleaseId());
        }
    }

    private static RuleItemEntity copyRule(RuleItemEntity original) {
        RuleItemEntity copy = new RuleItemEntity();
        copy.setRuleName(original.getRuleName());
        copy.setPackageName(original.getPackageName());
        copy.setAgenda(original.getAgenda());
        copy.setGlobals(original.getGlobals() == null
                ? null : new HashMap<>(original.getGlobals()));
        copy.setRuleContent(original.getRuleContent());
        return copy;
    }

    @Override
    public Set<String> getAllScenariosByRuleName(String ruleName) {
        return containerRules.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(rule -> rule.getRuleName().equals(ruleName)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public String getRuleContent(String ruleName) {
        Optional<RuleItemEntity> ruleItemEntity = containerRules.values().stream()
                .flatMap(Set::stream)
                .filter(filter -> filter.getRuleName().equals(ruleName))
                .findFirst();
        if (ruleItemEntity.isEmpty()) {
            String message = String.format("ruleName %s does not exists", ruleName);
            log.error(message);
            throw new RuleException(message, RuleValidationCode.RULE_NOT_FOUND);
        }
        return ruleItemEntity.get().getRuleContent();
    }

    @Override
    public Set<String> getAllScenarioName() {
        return containerRules.keySet();
    }

    @Override
    public LocalRuleSetDto getRuleEntitiesByScenario(String scenarioName) {
        if (scenarioName == null || scenarioName.isEmpty()) {
            String message = "scenarioName must not be null or empty";
            log.error(message);
            throw new ScenarioException(message, ScenarioValidationCode.scenario_NOT_FOUND);
        }
        Set<RuleItemEntity> ruleItemEntities = containerRules.getOrDefault(scenarioName, null);
        KieContainer container = containers.getOrDefault(scenarioName, null);
        if (container == null || ruleItemEntities == null || ruleItemEntities.isEmpty()) {
            String message = String.format("scenario name %s does not exist", scenarioName);
            log.error(message);
            throw new ScenarioException(message, ScenarioValidationCode.scenario_NOT_FOUND);
        }
        LocalRuleSetDto ruleSetDto = new LocalRuleSetDto();
        RuleScenarioEntity rulescenarioEntity = new RuleScenarioEntity();
        rulescenarioEntity.setScenario(scenarioName);
        rulescenarioEntity.setRuleItems(ruleItemEntities);
        ruleSetDto.setRuleScenarioEntity(rulescenarioEntity);
        ruleSetDto.setKieContainer(container);
        return ruleSetDto;
    }

    @Override
    public boolean validateDeleteRuleDto(DeleteScenarioBaseDto deleteRuleBaseDto) {
        try {
            deleteRuleBaseDto.validate();
        } catch (ScenarioException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean validateSaveRuleSetContext(LocalRuleSetDto saveRuleSetContext) {
        if (saveRuleSetContext.getKieContainer() == null) {
            log.error("kieContainer is required");
            return false;
        }
        if (saveRuleSetContext.getRulescenarioEntity() == null) {
            log.error("rulescenarioEntity is required");
            return false;
        }
        if (saveRuleSetContext.getRulescenarioEntity().getScenario() == null) {
            log.error("rulescenarioEntity scenario is required");
            return false;
        }
        RuleScenarioEntity rulescenarioEntity = saveRuleSetContext.getRulescenarioEntity();
        if (rulescenarioEntity.getRuleItems() == null || rulescenarioEntity.getRuleItems().isEmpty()) {
            log.error("ruleItems is required");
        }
        return true;
    }

    @Override
    public boolean isScenarioExists(String scenarioName) {
        return containerRules.containsKey(scenarioName);
    }

    @Override
    public boolean isRuleExists(String ruleName) {
        try {
            getRuleContent(ruleName);
            return true;
        } catch (RuleException e) {
            return false;
        }
    }


    private void validateOnFact(List<Object> facts) {
        if (facts == null || facts.isEmpty())
            throw new DroolsException("atLeast one fact is required", DroolsValidationCodeTmp.FACT_VARIABLE_NOT_FOUND);
    }
}
