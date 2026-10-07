package io.github.amirhosseinfsh.rulegate.domain.rule.service;

import io.github.amirhosseinfsh.rulegate.common.exception.DroolsException;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleItemDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.viewDto.RuleItemViewDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.viewDto.RuleViewDto;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.drl.parser.DrlParser;
import org.drools.drl.parser.DroolsParserException;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.definition.rule.Global;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.AgendaGroup;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp.*;

@Deprecated
public class RuleEngineService {

    private static final Logger log = LoggerFactory.getLogger(RuleEngineService.class);
    private static final KieServices ks = KieServices.Factory.get();

    private final DrlParser parser = new DrlParser();
    private final Map<String, KieContainer> containers = new HashMap<>();
    private final Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> containerRules = new HashMap<>();

    public RuleEngineService(boolean autoConfigureClassPath) {
        if (autoConfigureClassPath)
            startAutoConfigureFromClassPath();
    }

    private void startAutoConfigureFromClassPath() {
        String BASE_PATH = "src/main/resources/rules/";
        File baseDir = new File(BASE_PATH);
        if (baseDir.exists()) {
            File[] scenarioDirArray = baseDir.listFiles(file -> file != null && file.isDirectory());
            if (scenarioDirArray == null) return;
            for (File fileScenario : scenarioDirArray) {
                File[] drlArray = fileScenario.listFiles(file -> file.getName().endsWith(".drl"));
                if (drlArray == null) continue;
                String[] pathList = Arrays.stream(drlArray).map(File::getPath)
                        .map(item -> item.replace("src/main/resources/", "")).toArray(String[]::new);
                createContainerFromClasspath(fileScenario.getName(), pathList);
            }
        }

    }

    /**
     * @param scenarioName name of scenario that we want create
     * @param drlPaths     list for resourcePath that contain drools rules
     * @return name of scenario if create kie container is successful
     */
    public String createContainerFromClasspath(String scenarioName, String... drlPaths) {
        Assert.notNull(scenarioName, "scenarioName must not be null");
        Assert.notEmpty(drlPaths, "drlPaths must not be empty");
        if (isScenarioExists(scenarioName)) {
            log.warn("container '{}' already exists", scenarioName);
            return scenarioName;
        }
        RuleDto ruleDto = new RuleDto();
        ruleDto.setScenario(scenarioName);
        addRuleFromPath(ruleDto, drlPaths);
        return createContainerFromRuleDto(ruleDto);
    }


    /**
     * execute predefined rules with given scenarioName,list of facts ,map of global objects
     * global map must pass exactly global variables as needed with their actual name and type
     * drools will execute rules base on each fact , so checking on this facts list is optional
     *
     * @param facts   given as object but drools will automatically detect actual type and insert with that type
     * @param globals need to give exact names as key
     * @throws DroolsException with FACT_VARIABLE_NOT_FOUND -> must pass at least one variable as fact
     * @throws DroolsException with DROOLS_EXECUTION_ERROR -> (internal error occurred) must check error logs
     */
    public void execute(String scenarioName, List<Object> facts, Map<String, ?> globals) {
        validateExecuteInputs(scenarioName, facts);
        KieContainer kieContainer = containers.get(scenarioName);
        fireRuleSet(scenarioName, kieContainer, facts, globals, null);

    }

    private void fireRuleSet(String scenarioName, KieContainer kieContainer, List<Object> facts, Map<String, ?> globals, String agendaName) {
        long startTime = System.currentTimeMillis();
        KieSession kieSession = null;
        try {
            kieSession = kieContainer.newKieSession();
            ValidateAndApplyVariablesOnKieSession(kieSession, facts, globals);
            kieSession.fireAllRules();
            if (agendaName != null) {
                AgendaGroup agendaGroup = kieSession.getAgenda().getAgendaGroup(agendaName);
                if (agendaGroup != null) {
                    agendaGroup.setFocus();
                    kieSession.fireAllRules();
                }
            }
            log.debug("Executed {} facts with scenario '{}' in {}", facts.size(), scenarioName, System.currentTimeMillis() - startTime);

        } catch (RuntimeException e) {
            if (e instanceof DroolsException) throw e;
            log.error("Rule execution failed for scenario '{}'", scenarioName, e);
            throw new DroolsException("Rule execution failed", DROOLS_EXECUTION_ERROR);
        } finally {
            if (kieSession != null) kieSession.dispose();
        }
    }

    private void validateExecuteInputs(String scenarioName, List<Object> facts) {
        validateContainer(scenarioName);
        validateOnFact(facts);
    }

    public void execute(String scenarioName, List<Object> facts, Map<String, ?> globals, String agendaName) {
        validateExecuteInputs(scenarioName, facts);
        KieContainer kieContainer = containers.get(scenarioName);
        fireRuleSet(scenarioName, kieContainer, facts, globals, agendaName);
    }


    public Set<String> getAllScenarios() {
        return Collections.unmodifiableSet(containers.keySet());
    }

    public void clearAll() {
        containers.values().forEach(KieContainer::dispose);
        containers.clear();
        containerRules.clear();
        log.info("All rule scenario cleared");
    }

    public void deleteScenario(String scenarioName) {
        validateOnDeleteScenario(scenarioName);
        containers.remove(scenarioName);
        containerRules.remove(scenarioName);
        log.info("scenario {} deleted", scenarioName);
    }

    public void deleteRule(String scenarioName, String ruleName) {
        validateScenarioExists(scenarioName);
        Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> ruleSet = containerRules.get(scenarioName);
        ruleSet.removeIf(item -> item.getRuleName().equals(ruleName));
        KieContainer kieContainer = regenerate(scenarioName, ruleSet);
        containers.remove(scenarioName);
        containers.put(scenarioName, kieContainer);
        containerRules.put(scenarioName, ruleSet);
    }

    public RuleViewDto getRuleSet(String scenarioName) {
        validateScenarioExists(scenarioName);
        return fillViewDto(scenarioName, containerRules.get(scenarioName));
    }

    private void validateScenarioExists(String scenarioName) {
        if (!containerRules.containsKey(scenarioName)) {
            log.error("there is no scenarioId with value {}", scenarioName);
            throw new ScenarioException("there is no scenarioId with value " + scenarioName, DroolsValidationCodeTmp.SCENARIO_NOT_FOUND);
        }
    }

    /**
     * find rule with given ruleId
     *
     * @param ruleId @Description string type
     * @return RuleViewDto which has scenarioNames
     * @throws RuleException if ruleId does not exists
     */
    public Set<RuleViewDto> getRule(String ruleId) {
        Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> result = findByRuleId(ruleId, true);
        if (result.isEmpty()) {
            log.error("there is no rule with id {}", ruleId);
            throw new RuleException("there is no rule with id " + ruleId, DroolsValidationCodeTmp.RULE_NOT_FOUND);
        }
        return result.entrySet().stream().map(item -> fillViewDto(item.getKey()
                , item.getValue())).collect(Collectors.toSet());

    }

    /**
     * this method will iterate over all ruleSets of every scenario and will update affected scenario ruleSets
     *
     * @param ruleContent ruleName inside param ruleContent must exists in ruleSet
     * @throws RuleException if given ruleContent is not parsable and have syntax errors or ruleName inside ruleContent does not do exists
     */
    public void updateRule(String ruleContent) throws RuleException {
        validateRuleContent(ruleContent);
        String ruleName = getRuleNameFromContent(ruleContent);
        Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> ruleMap = validateRuleExist(ruleName);
        applyUpdatesOnMap(ruleMap, ruleName, ruleContent);
        Map<String, KieContainer> changedContainerMap = ruleMap.entrySet().stream()
                .map(changedEntry -> {
                    String scenarioName = changedEntry.getKey();
                    Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> ruleEntrySet = changedEntry.getValue();


                    RuleDto ruleDto = new RuleDto();
                    ruleDto.setScenario(scenarioName);
                    ruleDto.setRuleItems(ruleEntrySet.stream()
                            .map(i -> new RuleItemDto(i.getRuleContent()))
                            .collect(Collectors.toSet()));

                    KieFileSystem kfs = getKieFileSystemFromStringRules(ruleDto);
                    return Map.of(scenarioName, buildAndRegisterContainer(kfs));
                }).flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        containers.putAll(changedContainerMap);
        containerRules.putAll(ruleMap);
    }

    private Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> validateRuleExist(String ruleName) {
        Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> ruleMap = findByRuleId(ruleName, false);
        if (ruleMap.isEmpty()) {
            log.error("there is no rule with name {}", ruleName);
            throw new RuleException(String.format("there is no rule with name %s", ruleName), RULE_NOT_FOUND, ruleName);
        }
        return ruleMap;
    }

    private void validateRuleContent(String ruleContent) {
        if (ruleContent == null || ruleContent.isBlank()) {
            log.error("ruleContent is null or empty");
            throw new RuleException("ruleContent is null or empty", DroolsValidationCodeTmp.RULE_NOT_FOUND);
        }
    }

    private void applyUpdatesOnMap(Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> ruleMap, String ruleName, String ruleContent) {
        ruleMap.forEach((key, value) -> value
                .forEach(itemRule -> {
                    if (itemRule.getRuleName().equals(ruleName))
                        itemRule.setRuleContent(ruleContent);
                }));
    }

    /**
     * use this method only for adding existing rules to the scenario with their names
     *
     * @param scenarioName string
     * @param ruleNameSet  list of string
     * @throws RuleException with RULE_NOT_FOUND if given ruleNameSet does not do exists in whole rule sets
     */
    public void addRuleToScenario(String scenarioName, Set<String> ruleNameSet) {
        var validations = ruleNameSet.stream().filter(it -> findByRuleId(it, true).isEmpty())
                .peek(item -> log.error("there is no rule with id {}", item)).collect(Collectors.toSet());

        if (!validations.isEmpty())
            throw new RuleException("there are no rules with given ids", RULE_NOT_FOUND, validations);

        Set<RuleItemDto> ruleItemDtoList = ruleNameSet.stream().map(item -> getRule(item).stream().findFirst()
                        .get()).map(item -> item.getRuleItems().stream().findFirst().get().getRuleContent())
                .map(RuleItemDto::new).collect(Collectors.toSet());

        RuleDto ruleDto = new RuleDto();
        ruleDto.setScenario(scenarioName);
        ruleDto.setRuleItems(ruleItemDtoList);
        addRuleToScenario(ruleDto);
    }

    /**
     * use this method only for adding new rules with new definitions
     *
     * @param ruleDto has scenarioId and list of ruleContent to be newly added
     * @throws ScenarioException with scenario_NOT_FOUND if given scenario in ruleDto does not do exists
     * @throws DroolsException   with DUPLICATE_RULE_FOUND if ruleContent inside ruleDto has any same ruleName inside ruleSet
     */
    public void addRuleToScenario(RuleDto ruleDto) {
        String scenarioName = ruleDto.getScenario();
        Set<String> ruleItemDtoSet = ruleDto.getRuleItems().stream()
                .map(RuleItemDto::getRuleContent)
                .map(this::getRuleNameFromContent)
                .collect(Collectors.toSet());
        if (!containers.containsKey(scenarioName)) {
            log.error("scenario not found {}", scenarioName);
            throw new ScenarioException(String.format("scenario not found %s", scenarioName), SCENARIO_NOT_FOUND, scenarioName);
        }
        long errorCount = containerRules.entrySet().stream().filter(i -> i.getValue().stream().anyMatch(it -> {
            boolean isSame = ruleItemDtoSet.contains(it.getRuleName());
            if (isSame)
                log.error("rule name {} still exists for scenario name {}, please try adding rules by their ruleName instead of content", it.getRuleName(), i.getKey());
            return isSame;
        })).count();
        if (errorCount != 0)
            throw new DroolsException("can not add new rules with existing same names", DroolsValidationCodeTmp.DUPLICATE_RULE_FOUND);

        RuleDto newRuleDto = new RuleDto();
        newRuleDto.setScenario(scenarioName);

        Set<RuleItemDto> beforeRuleSet = containerRules.get(scenarioName).stream().map(item -> new RuleItemDto(item.getRuleContent())).collect(Collectors.toSet());
        Set<RuleItemDto> newRuleSet = Stream.concat(beforeRuleSet.stream(), ruleDto.getRuleItems().stream()).collect(Collectors.toSet());

        newRuleDto.setRuleItems(newRuleSet);


        createContainerFromRuleDto(newRuleDto);
    }

    private void validateOnDeleteScenario(String scenarioName) {
        if (!containers.containsKey(scenarioName) || !containerRules.containsKey(scenarioName)) {
            throw new DroolsException("scenario not found", DroolsValidationCodeTmp.SCENARIO_NOT_FOUND, scenarioName);
        }
    }

    public String createContainerFromRuleDto(RuleDto ruleDto) {
        String scenarioName = ruleDto.getScenario();
        KieFileSystem kfs = getKieFileSystemFromStringRules(ruleDto);
        KieContainer kieContainer = buildAndRegisterContainer(kfs);

        Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> rules = ruleDto.getRuleItems().stream()
                .map(this::fillItemEntity)
                .collect(Collectors.toSet());

        containers.put(scenarioName, kieContainer);
        containerRules.put(scenarioName, rules);
        return scenarioName;
    }

    private void validateOnFact(List<Object> facts) {
        if (facts == null || facts.isEmpty())
            throw new DroolsException("atLeast one fact is required", DroolsValidationCodeTmp.FACT_VARIABLE_NOT_FOUND);
    }

    /**
     * get ruleDto and make validation on syntax and then returns a KieFileSystem from given ruleSet
     *
     * @param ruleDto
     * @return KieFileSystem which has ruleSet
     * @throws RuleException   if given rule has syntax error
     * @throws DroolsException if given rule has conflicts with other rules in given ruleSet of scenario
     */
    private KieFileSystem getKieFileSystemFromStringRules(RuleDto ruleDto) {

        Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> rules = ruleDto.getRuleItems().stream().map(this::fillItemEntity).collect(Collectors.toSet());
        try {
            KieFileSystem kfs = ks.newKieFileSystem();
            writeOnKieFileSystem(kfs, rules);
            validateRuleSet(ruleDto, rules);
            return kfs;
        } catch (Exception e) {
            log.error("Failed to create KieContainer  from String", e);
            if (e instanceof DroolsException) throw (DroolsException) e;
            throw new DroolsException("Error creating kieContainer " + e.getLocalizedMessage(), DROOLS_BUILD_ERROR);
        }
    }

    /// ========== PRIVATE HELPER METHOD ================

    private void writeOnKieFileSystem(KieFileSystem kfs, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> rules) {
        rules.forEach((itemRule) -> {
            String ruleContent = itemRule.getRuleContent();
            PackageDescr packageDescr = null;
            try {
                packageDescr = parser.parse(null, ruleContent);
            } catch (DroolsParserException e) {
                log.error(e.getMessage());
                throw new RuleException(String.format("there is an error with your %s.drl file", ruleContent), RULE_NOT_FOUND);
            }
            String packName = packageDescr.getName();
            String kiePath = String.format("src/main/resources/%s/%s.drl", (packName == null ? "" : packName.replace('.', '/').replace(";", "")), itemRule.getRuleName());
            kfs.write(kiePath, ruleContent);
            log.info("Adding DRL content for rule {}", itemRule.getRuleName());
        });
    }


    /**
     * make final build from given KieFileSystem and returns KieContainer
     *
     * @param kfs
     * @return
     * @throws DroolsException if ruleSet written inside kfs can not be compiled due to syntax errors or conflicts
     */
    private KieContainer buildAndRegisterContainer(KieFileSystem kfs) {
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        Results results = kieBuilder.getResults();
        if (results.hasMessages(Message.Level.ERROR)) {
            String errors = compileErrorToString(results);
            log.error("Drools compilation error: {}", errors);
            throw new DroolsException("Rule compilation failed: ", DROOLS_BUILD_ERROR, errors);
        }

        return ks.newKieContainer(ks.getRepository().getDefaultReleaseId());
    }


    private String getRuleNameFromContent(String ruleContent) {
        try {
            PackageDescr packageDescr = parser.parse(null, ruleContent);
            validateDroolsSyntax(packageDescr);
            return packageDescr.getRules().getFirst().getName();
        } catch (DroolsParserException e) {
            log.error("rule name not found");
            throw new RuleException("rule name not found", DroolsValidationCodeTmp.RULE_NOT_FOUND);
        }
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

    private Map<String, String> getRequiredMap(KieSession kieSession) {
        return kieSession.getKieBase().getKiePackages().stream().flatMap(item -> item.getGlobalVariables()
                .stream()).collect(Collectors.toMap(Global::getName, Global::getType));
    }

    private KieContainer regenerate(String scenarioName, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> rules) {
        Set<RuleItemDto> ruleSet = rules.stream().map(item -> new RuleItemDto(item.getRuleContent())).collect(Collectors.toSet());
        RuleDto ruleDto = new RuleDto();
        ruleDto.setScenario(scenarioName);
        ruleDto.setRuleItems(ruleSet);
        KieFileSystem kfs = getKieFileSystemFromStringRules(ruleDto);
        return buildAndRegisterContainer(kfs);
    }

    private void validateRuleSet(RuleDto ruleDto, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> rules) {
        List<String> validations = new ArrayList<>();
        Map<String, String> globals = new HashMap<>();
        Set<String> seen = new HashSet<>();
        List<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> duplicates = rules.stream().peek(itemRule -> {
            try {
                parser.parse(null, itemRule.getRuleContent()).getGlobals().forEach(item -> {
                    String value = globals.getOrDefault(item.getIdentifier(), null);
                    if (value == null || value.equals(item.getType())) {
                        globals.put(item.getIdentifier(), item.getType());
                        return;
                    }
                    String errorParam = String.format("%s %s", item.getType(), item.getIdentifier());
                    log.error("duplicate global variable '{}' found", errorParam);
                    validations.add(errorParam);
                });
            } catch (DroolsParserException e) {
                throw new RuntimeException(e);
            }
        }).filter(obj -> !seen.add(obj.getRuleName())).toList();

        if (!duplicates.isEmpty()) {
            duplicates.forEach(item -> log.error("{} repeated multiple times for given scenarioId {}", item.getRuleName()
                    , ruleDto.getScenario()));
            throw new ScenarioException(String.format("multiple rules were found for scenarioId %s", ruleDto.getScenario())
                    , MULTIPLE_SAME_RULES_FOUND, duplicates.stream().map(io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity::getRuleName).toList());
        }

        if (!validations.isEmpty()) {
            throw new DroolsException("", GLOBAL_VARIABLE_DUPLICATED_FOUND, validations);
        }


    }

    private void validateDroolsSyntax(PackageDescr packageDescr) {
        validatePackageDescNull(packageDescr);
        validatePackageDescriberName(packageDescr);
        validateEmptyPackageDesRule(packageDescr);
        validateExtraRules(packageDescr);
    }

    private Map<String, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity>> findByRuleId(String ruleId, boolean filterRuleIdOnResult) {
        return containerRules.entrySet().stream()
                .filter(item -> item.getValue().stream().map(io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity::getRuleName)
                        .anyMatch(ruleItem -> ruleItem.equals(ruleId)))
                .collect(Collectors.toMap(Map.Entry::getKey
                        , entry -> filterRuleIdOnResult ? entry.getValue().stream()
                                .filter(i -> i.getRuleName().equals(ruleId)).collect(Collectors.toSet()) : entry.getValue()));
    }

    private RuleViewDto fillViewDto(String scenarioName, Set<io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity> ruleItemEntity) {
        var ruleItemViewDtoList = ruleItemEntity.stream().map(
                item -> new RuleItemViewDto(item.getRuleName(), item.getRuleContent())
        ).collect(Collectors.toSet());
        RuleViewDto ruleViewDto = new RuleViewDto();
        ruleViewDto.setScenario(scenarioName);
        ruleViewDto.setRuleItems(ruleItemViewDtoList);
        return ruleViewDto;
    }

    private io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity fillItemEntity(RuleItemDto ruleItemDto) {
        io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity ruleItemEntity = new io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity();
        ruleItemEntity.setRuleContent(ruleItemDto.getRuleContent());
        ruleItemEntity.setRuleName(getRuleNameFromContent(ruleItemDto.getRuleContent()));
        return ruleItemEntity;
    }

    private void validateContainer(String scenarioName) {
        if (!isScenarioExists(scenarioName)) {
            String error = String.format("scenario %s does not exist", scenarioName);
            throw new DroolsException(error, DROOLS_EXECUTION_ERROR, scenarioName);
        }
    }


    private String compileErrorToString(Results results) {
        StringBuilder sb = new StringBuilder();
        results.getMessages(Message.Level.ERROR).forEach(msg -> sb.append("Line ").append(msg.getLine()).append(": ").append(msg.getText()).append("\n"));
        return sb.toString();
    }

    private void addRuleFromPath(RuleDto ruleDto, String[] drlPaths) {
        for (String drlPath : drlPaths) {
            String content = resourcePathToString(drlPath);
            ruleDto.getRuleItems().add(new RuleItemDto(content));
        }
    }

    private String resourcePathToString(String resourcePath) {

        try (InputStream is = ResourceFactory.newClassPathResource(resourcePath).getInputStream()) {
            if (is == null) throw new RuntimeException("Can't find resource: " + resourcePath);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DroolsException("error for reading path:  " + resourcePath + " " + e.getMessage(), DRL_PATH_NOT_FOUND);
        }
    }

    private void ValidateAndApplyVariablesOnKieSession(KieSession kieSession, List<Object> facts, Map<String, ?> globals) {
        validateOnFact(facts);
        validateGlobalAndSet(kieSession, globals);
        kieSession.execute(CommandFactory.newInsertElements(facts));
    }


    private boolean isScenarioExists(String scenarioName) {
        return containers.containsKey(scenarioName);
    }

    private void validateEmptyPackageDesRule(PackageDescr packageDescr) {
        if (packageDescr.getRules().isEmpty()) {
            log.error("rules are empty");
            throw new RuleException("rules are empty", DroolsValidationCodeTmp.RULE_NOT_FOUND);
        }
    }

    private void validateExtraRules(PackageDescr packageDescr) {
        if (packageDescr.getRules().size() > 1) {
            log.error("there is more than one rule in a set");
            throw new RuleException("there is more than one rule in a set", DroolsValidationCodeTmp.MULTIPLE_RULES_FOUND);
        }
    }

    private void validatePackageDescNull(PackageDescr pd) {
        if (pd == null) {
            String error = "can not parse drl, provided rule content is not a real rule please follow the right syntax";
            log.error(error);
            throw new RuleException(error, RULE_NOT_FOUND);
        }
    }

    private void validatePackageDescriberName(PackageDescr pd) {
        if (pd.getName() == null || pd.getName().isBlank()) {
            log.error("package name is null or empty");
            throw new RuleException("package name is null or empty", DroolsValidationCodeTmp.PACKAGE_NOT_FOUND);
        }
    }

}
