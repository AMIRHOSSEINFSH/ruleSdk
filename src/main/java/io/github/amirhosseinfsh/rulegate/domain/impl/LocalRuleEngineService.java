package io.github.amirhosseinfsh.rulegate.domain.impl;

import io.github.amirhosseinfsh.rulegate.api.context.RuleEngineServiceContext;
import io.github.amirhosseinfsh.rulegate.api.datasource.DroolsRepository;
import io.github.amirhosseinfsh.rulegate.api.dto.DeleteRuleBaseDto;
import io.github.amirhosseinfsh.rulegate.api.dto.DeleteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.api.dto.ExecuteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.common.exception.DroolsException;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import io.github.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;
import io.github.amirhosseinfsh.rulegate.domain.dto.LocalRuleSetDto;
import io.github.amirhosseinfsh.rulegate.domain.dto.builder.LocalCreateRuleSetDtoBuilder;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleItemDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity;
import io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;
import io.github.amirhosseinfsh.rulegate.domain.rule.viewDto.RuleItemViewDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.viewDto.RuleViewDto;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.drl.ast.descr.RuleDescr;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.definition.rule.Global;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import io.github.amirhosseinfsh.rulegate.execution.RuleExecutionResult;
import io.github.amirhosseinfsh.rulegate.execution.RuleValidation;
import org.kie.internal.command.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp.DROOLS_BUILD_ERROR;
import static io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp.DROOLS_EXECUTION_ERROR;

public class LocalRuleEngineService implements RuleEngineServiceContext<LocalRuleSetDto, DeleteScenarioBaseDto> {

    private static final Logger log = LoggerFactory.getLogger(LocalRuleEngineService.class);
    private final DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> dataSourceRepository;
    private final DroolsHelper droolsHelper = new DroolsHelper();
    private final KieServices ks;
    private final String RULE_DIR;
    private final ResourcePatternResolver resolver;
    private final boolean strictValidationMetadata;


    public LocalRuleEngineService(DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> dataSourceRepository
            , boolean autoConfigureClassPath) {
        this(dataSourceRepository, autoConfigureClassPath, false);
    }

    public LocalRuleEngineService(DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> dataSourceRepository
            , boolean autoConfigureClassPath, boolean strictValidationMetadata) {
        this(dataSourceRepository, autoConfigureClassPath, "rules", new PathMatchingResourcePatternResolver(),
                strictValidationMetadata);
    }

    public LocalRuleEngineService(DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> dataSourceRepository
            , boolean autoConfigureClassPath, String ruleDir, ResourcePatternResolver resolver) {
        this(dataSourceRepository, autoConfigureClassPath, ruleDir, resolver, false);
    }

    public LocalRuleEngineService(DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> dataSourceRepository
            , boolean autoConfigureClassPath, String ruleDir, ResourcePatternResolver resolver,
                                  boolean strictValidationMetadata) {
        this.resolver = resolver;
        this.RULE_DIR = ruleDir;
        this.dataSourceRepository = dataSourceRepository;
        this.strictValidationMetadata = strictValidationMetadata;
        ks = KieServices.Factory.get();
        if (autoConfigureClassPath)
            startAutoConfigureFromClassPath();
    }

    private KieFileSystem getKieFileSystemFromStringRules(RuleDto ruleDto) {
        Set<RuleItemEntity> rules = convertToEntity(ruleDto).getRuleItems();
        try {
            KieFileSystem kfs = ks.newKieFileSystem();
            writeOnKieFileSystem(kfs, rules);
            validateRuleSet(ruleDto.getRuleItems().stream().map(RuleItemDto::getRuleContent).toList());
            return kfs;
        } catch (Exception e) {
            if (e instanceof DroolsException droolsException) {
                log.warn("Cannot build local rules: {}", e.getMessage());
                throw droolsException;
            }
            log.error("Failed to create KieContainer from String", e);
            throw new DroolsException("Error creating kieContainer " + e.getLocalizedMessage(), DROOLS_BUILD_ERROR);
        }
    }

    private void writeOnKieFileSystem(KieFileSystem kfs, Set<RuleItemEntity> rules) {
        rules.forEach(ruleItemEntity -> {
            String ruleContent = ruleItemEntity.getRuleContent();
            PackageDescr packageDescr = droolsHelper.parseDrl(ruleItemEntity.getRuleContent());
            String packName = droolsHelper.getPackageName(packageDescr);
            String kiePath = String.format("src/main/resources/%s/%s.drl",
                    (packName == null ? "" : packName.replace('.', '/').replace(";", "")),
                    ruleItemEntity.getRuleName());
            kfs.write(kiePath, ruleContent);
            log.info("Adding DRL content for rule {}", ruleItemEntity.getRuleName());
        });
    }

    @Override
    public DroolsRepository<LocalRuleSetDto, DeleteScenarioBaseDto> getDataSourceRepository() {
        return dataSourceRepository;
    }

    public boolean isStrictValidationMetadata() {
        return strictValidationMetadata;
    }

    public void createScenarioFromClassPath(String scenarioName, String... drlPaths) {
        Assert.notNull(scenarioName, "scenarioName must not be null");
        Assert.notEmpty(drlPaths, "drlPaths must not be empty");
        if (isScenarioExists(scenarioName)) {
            String message = String.format("scenario %s already exists", scenarioName);
            log.error(message);
            throw new ScenarioException(message, ScenarioValidationCode.scenario_MUST_NOT_EXISTS);
        }
        List<String> ruleContentList = droolsHelper.getRulesFromFiles(drlPaths);
        validateRuleSet(ruleContentList);
        RuleDto createRuleDto = new RuleDto();
        createRuleDto.setScenario(scenarioName);
        createRuleDto.setRuleItems(fillRuleItemSet(ruleContentList));
        createScenario(createRuleDto);
    }


    @Override
    public void createScenario(RuleDto ruleDto) {
        KieFileSystem kfs = getKieFileSystemFromStringRules(ruleDto);
        KieContainer kieContainer = buildAndRegisterContainer(kfs);
        RuleScenarioEntity rulescenarioEntity = convertToEntity(ruleDto);
        LocalRuleSetDto ruleSetContext = LocalCreateRuleSetDtoBuilder.Builder()
                .withRuleScenarioEntity(rulescenarioEntity)
                .withKieContainer(kieContainer)
                .build();
        dataSourceRepository.createRuleScenario(ruleSetContext);
    }

    private KieContainer buildAndRegisterContainer(KieFileSystem kfs) {
        var releaseId = ks.newReleaseId(
                "io.github.amirhosseinfsh.rulegate", "rule-sdk-local-rules", UUID.randomUUID().toString());
        kfs.generateAndWritePomXML(releaseId);
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            String errors = compileErrorToString(results);
            log.error("Drools compilation error: {}", errors);
            throw new DroolsException("Rule compilation failed: ", DROOLS_BUILD_ERROR, errors);
        }

        return ks.newKieContainer(releaseId);
    }

    private String compileErrorToString(Results results) {
        StringBuilder sb = new StringBuilder();
        results.getMessages(Message.Level.ERROR).forEach(msg -> sb.append("Line ").append(msg.getLine()).append(": ").append(msg.getText()).append("\n"));
        return sb.toString();
    }


    @Override
    public void deleteRule(DeleteRuleBaseDto deleteRuleBaseDto) {
        deleteRuleBaseDto.validate();
        String ruleName = deleteRuleBaseDto.getRuleName();
        String scenarioName = deleteRuleBaseDto.getScenarioName();
        LocalRuleSetDto localRuleSetDto = dataSourceRepository.getRuleEntitiesByScenario(scenarioName);
        Set<RuleItemEntity> ruleItemEntitySet = localRuleSetDto.getRulescenarioEntity().getRuleItems();
        ruleItemEntitySet.removeIf(item -> item.getRuleName().equals(ruleName));
        KieContainer kieContainer = regenerate(scenarioName, ruleItemEntitySet);
        LocalRuleSetDto ruleSetDto = LocalCreateRuleSetDtoBuilder.Builder()
                .withRuleScenarioEntity(localRuleSetDto.getRulescenarioEntity())
                .withKieContainer(kieContainer)
                .build();
        dataSourceRepository.createRuleScenario(ruleSetDto);
    }

    private KieContainer regenerate(String scenarioName, Set<RuleItemEntity> rules) {
        Set<RuleItemDto> ruleSet = rules.stream().map(item -> new RuleItemDto(item.getRuleContent())).collect(Collectors.toSet());
        RuleDto ruleDto = new RuleDto();
        ruleDto.setScenario(scenarioName);
        ruleDto.setRuleItems(ruleSet);
        KieFileSystem kfs = getKieFileSystemFromStringRules(ruleDto);
        return buildAndRegisterContainer(kfs);
    }

    @Override
    public void deleteScenario(DeleteScenarioBaseDto deleteScenarioBaseDto) {
        deleteScenarioBaseDto.validate();
        String scenarioName = deleteScenarioBaseDto.getScenarioName();
        if (!dataSourceRepository.isScenarioExists(scenarioName)) {
            log.error("scenario {} does not exist", scenarioName);
            throw new ScenarioException(String.format("scenario %s does not exists", scenarioName), ScenarioValidationCode.scenario_NOT_FOUND);
        }
        dataSourceRepository.deleteScenario(deleteScenarioBaseDto);
    }

    @Override
    public void execute(ExecuteScenarioBaseDto executeScenarioDto) {
        executeForResult(executeScenarioDto);
    }

    public RuleExecutionResult executeForResult(ExecuteScenarioBaseDto executeScenarioDto) {
        executeScenarioDto.validate();
        String scenarioName = executeScenarioDto.getScenarioName();
        List<Object> facts = executeScenarioDto.getFacts();
        validateExecuteInputs(scenarioName, facts);
        KieContainer container = dataSourceRepository.getRuleEntitiesByScenario(scenarioName).getKieContainer();
        return fireRuleSet(scenarioName, container, facts,
                executeScenarioDto.getGlobals(), executeScenarioDto.getAgendaGroupName());
    }

    private RuleExecutionResult fireRuleSet(String scenarioName, KieContainer container,
                                            List<Object> facts, Map<String, ?> globals,
                                            String agendaName) {
        long startTime = System.currentTimeMillis();
        KieSession session = null;
        List<RuleValidation> validations = new ArrayList<>();
        try {
            session = container.newKieSession();
            session.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    var rule = event.getMatch().getRule();
                    var metadata = rule.getMetaData();
                    Object errorValue = metadata.get("ERROR");
                    String type = errorValue == null ? null
                            : (Boolean.parseBoolean(String.valueOf(errorValue)) ? "ERROR" : "WARNING");
                    Object messageValue = metadata.get("MESSAGE");
                    String message = messageValue == null ? null : String.valueOf(messageValue);
                    validations.add(new RuleValidation(
                            rule.getName(), message, type));
                }
            });
            ValidateAndApplyVariablesOnKieSession(session, facts, globals);
            if (agendaName != null && !agendaName.isBlank()) {
                session.getAgenda().getAgendaGroup(agendaName).setFocus();
            }
            session.fireAllRules();
            log.debug("Executed {} facts with scenario '{}' in {}",
                    facts.size(), scenarioName, System.currentTimeMillis() - startTime);
            return new RuleExecutionResult(scenarioName, validations);
        } catch (RuntimeException e) {
            if (e instanceof DroolsException) throw e;
            log.error("Rule execution failed for scenario '{}'", scenarioName, e);
            throw new DroolsException("Rule execution failed", DROOLS_EXECUTION_ERROR);
        } finally {
            if (session != null) session.dispose();
        }
    }

    private void ValidateAndApplyVariablesOnKieSession(KieSession kieSession, List<Object> facts, Map<String, ?> globals) {
        validateOnFact(facts);
        validateGlobalAndSet(kieSession, globals);
        kieSession.execute(CommandFactory.newInsertElements(facts));
    }

    private void validateGlobalAndSet(KieSession kieSession, Map<String, ?> globals) {
        Map<String, String> requiredGlobalMap = getRequiredMap(kieSession);
        List<String> validations = checkRequiredGlobal(globals.keySet(), requiredGlobalMap.keySet());
        validations.addAll(checkExtraGlobal(globals.keySet(), requiredGlobalMap.keySet()));
        if (!validations.isEmpty())
            throw new DroolsException(String.format("global variables %s are mis matched", validations)
                    , DroolsValidationCodeTmp.GLOBAL_VARIABLE_NOT_FOUND, validations);
        for (Map.Entry<String, ?> stringEntry : globals.entrySet())
            kieSession.setGlobal(stringEntry.getKey(), stringEntry.getValue());
    }

    private Map<String, String> getRequiredMap(KieSession kieSession) {
        return kieSession.getKieBase().getKiePackages().stream().flatMap(item -> item.getGlobalVariables()
                .stream()).collect(Collectors.toMap(Global::getName, Global::getType));
    }

    private List<String> checkExtraGlobal(Set<String> globalName, Set<String> requiredGlobalName) {
        List<String> validations = new ArrayList<>();
        requiredGlobalName.stream()
                .filter(item -> !globalName.contains(item))
                .forEach(item -> {
                    log.error("global variable {} did not needed", item);
                    validations.add(item);
                });
        return validations;
    }

    private List<String> checkRequiredGlobal(Set<String> globalsNames, Set<String> requiredGlobalsName) {
        List<String> validations = new ArrayList<>();
        requiredGlobalsName.stream()
                .filter(globalName -> !globalsNames.contains(globalName))
                .forEach(globalName -> {
                    log.error("global variable {} did not passed", globalName);
                    validations.add(globalName);
                });
        return validations;
    }

    private void validateExecuteInputs(String scenarioName, List<Object> facts) {
        validateContainer(scenarioName);
        validateOnFact(facts);
    }

    private void validateOnFact(List<Object> facts) {
        if (facts == null || facts.isEmpty())
            throw new DroolsException("atLeast one fact is required", DroolsValidationCodeTmp.FACT_VARIABLE_NOT_FOUND);
    }

    private void validateContainer(String scenarioName) {
        if (!dataSourceRepository.isScenarioExists(scenarioName)) {
            String error = String.format("scenario %s does not exist", scenarioName);
            throw new DroolsException(error, DROOLS_EXECUTION_ERROR, scenarioName);
        }
    }

    @Override
    public Set<String> getAllScenarios() {
        return dataSourceRepository.getAllScenarioName();
    }

    @Override
    public RuleViewDto getRuleSetByScenarioName(String scenarioName) {
        LocalRuleSetDto localRuleSetDto = dataSourceRepository.getRuleEntitiesByScenario(scenarioName);
        return convertToRuleViewDto(localRuleSetDto.getRulescenarioEntity());
    }

    @Override
    public String getRuleContext(String ruleId) {
        return dataSourceRepository.getRuleContent(ruleId);
    }

    @Override
    public void updateRule(String ruleContent) {
        validateOnUpdateRule(ruleContent);
        validateValidationMetadata(List.of(ruleContent));
        dataSourceRepository.updateRuleContent(droolsHelper.getRuleName(ruleContent), ruleContent);
    }


    private void validateOnUpdateRule(String ruleContent) {
        String ruleName = droolsHelper.getRuleName(ruleContent);
        if (!dataSourceRepository.isRuleExists(ruleName)) {
            String message = String.format("rule %s does not exist", ruleName);
            log.error(message);
            throw new RuleException(message, RuleValidationCode.RULE_NOT_FOUND);
        }
    }

    @Override
    public void addRuleToScenario(RuleDto ruleDto) {

    }

    @Override
    public boolean isScenarioExists(String scenarioName) {
        return dataSourceRepository.isScenarioExists(scenarioName);
    }

    @Override
    public void validateRuleSet(List<String> ruleContentList) {
        droolsHelper.validateDuplicateRules(ruleContentList);
        droolsHelper.validateDuplicateGlobalObjects(ruleContentList);
        validateValidationMetadata(ruleContentList);
    }

    private void validateValidationMetadata(List<String> ruleContentList) {
        if (strictValidationMetadata) {
            for (String ruleContent : ruleContentList) {
                for (RuleDescr rule : droolsHelper.parseDrl(ruleContent).getRules()) {
                    if (!rule.hasAnnotation("ERROR") || !rule.hasAnnotation("MESSAGE")) {
                        throw new RuleException("Rule '" + rule.getName()
                                + "' requires both @ERROR and @MESSAGE annotations",
                                RuleValidationCode.RULE_VALIDATION_METADATA_MISSING);
                    }
                    Object errorValue = rule.getAnnotation("ERROR").getSingleValueStripped();
                    Object messageValue = rule.getAnnotation("MESSAGE").getSingleValueStripped();
                    String errorText = errorValue == null ? null : String.valueOf(errorValue);
                    if ((errorText == null || !("true".equalsIgnoreCase(errorText)
                            || "false".equalsIgnoreCase(errorText)))
                            || messageValue == null || String.valueOf(messageValue).isBlank()) {
                        throw new RuleException("Rule '" + rule.getName()
                                + "' requires @ERROR(true/false) and a non-empty @MESSAGE",
                                RuleValidationCode.RULE_VALIDATION_METADATA_INVALID);
                    }
                }
            }
        }
    }


    private Set<RuleItemDto> fillRuleItemSet(List<String> ruleContentList) {
        return ruleContentList.stream()
                .map(RuleItemDto::new)
                .collect(Collectors.toSet());
    }

    private RuleScenarioEntity convertToEntity(RuleDto ruleDto) {
        RuleScenarioEntity rulescenarioEntity = new RuleScenarioEntity();
        rulescenarioEntity.setScenario(ruleDto.getScenario());
        ruleDto.getRuleItems().forEach(item -> rulescenarioEntity.getRuleItems()
                .add(convertToRuleItemEntity(item.getRuleContent())));
        return rulescenarioEntity;
    }

    private RuleItemEntity convertToRuleItemEntity(String ruleContent) {
        RuleItemEntity ruleItemEntity = new RuleItemEntity();
        ruleItemEntity.setRuleName(droolsHelper.getRuleName(ruleContent));
        ruleItemEntity.setPackageName(droolsHelper.getPackageName(ruleContent));
        ruleItemEntity.setRuleContent(ruleContent);
        ruleItemEntity.setGlobals(droolsHelper.getGlobalsFromRuleContent(ruleContent));
        return ruleItemEntity;
    }

    private void startAutoConfigureFromClassPath() {
        try {
            Resource[] resources = resolver.getResources("classpath:" + RULE_DIR + "/**");
            Map<String, List<String>> rulesByDirectory = new TreeMap<>();
            for (Resource resource : resources) {
                if (resource.getFilename() != null && resource.getFilename().endsWith(".drl") && resource.isReadable()) {
                    String fullPath = resource.getURI().toString();
                    String relativePath = fullPath.substring(fullPath.indexOf(RULE_DIR + "/"));
                    String dirName = relativePath.substring(relativePath.indexOf("/") + 1, relativePath.lastIndexOf('/'));
                    rulesByDirectory.computeIfAbsent(dirName, k -> new ArrayList<>()).add(relativePath);
                }
            }
            rulesByDirectory.forEach((scenario, drlPaths) ->
                    createScenarioFromClassPath(scenario, drlPaths.toArray(new String[0])));
            log.info("Scenario list  : {}loaded successfully", rulesByDirectory.keySet());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String[] extractPathList(File[] drlArray) {
        return Arrays.stream(drlArray).map(File::getPath)
                .map(item -> {
                    int rulesIndex = item.lastIndexOf(RULE_DIR);
                    return rulesIndex != -1 ? item.substring(rulesIndex) : item;
                }).toArray(String[]::new);
    }

    @Override
    public RuleViewDto convertToRuleViewDto(RuleScenarioEntity rulescenarioEntity) {
        RuleViewDto ruleViewDto = new RuleViewDto();
        ruleViewDto.setScenario(rulescenarioEntity.getScenario());
        Set<RuleItemViewDto> ruleItemViewDtoSet = rulescenarioEntity.getRuleItems()
                .stream()
                .map(item -> {
                    RuleItemViewDto itemViewDto = new RuleItemViewDto();
                    itemViewDto.setRuleId(item.getRuleName());
                    itemViewDto.setRuleContent(item.getRuleContent());
                    itemViewDto.setPackageName(item.getPackageName());
                    itemViewDto.setGlobals(item.getGlobals());
                    return itemViewDto;
                }).collect(Collectors.toSet());
        ruleViewDto.setRuleItems(ruleItemViewDtoSet);
        return ruleViewDto;
    }
}
