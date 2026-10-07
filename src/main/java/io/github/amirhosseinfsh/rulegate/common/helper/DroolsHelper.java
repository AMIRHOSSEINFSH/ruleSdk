package io.github.amirhosseinfsh.rulegate.common.helper;

import io.github.amirhosseinfsh.rulegate.common.exception.DroolsException;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import io.github.amirhosseinfsh.rulegate.domain.fact.FactDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleItemEntity;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.drools.drl.ast.descr.GlobalDescr;
import org.drools.drl.ast.descr.PackageDescr;
import org.drools.drl.ast.descr.RuleDescr;
import org.drools.drl.parser.DrlParser;
import org.drools.drl.parser.DroolsParserException;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp.DROOLS_BUILD_ERROR;
import static io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode.RULE_NOT_FOUND;
import static io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode.RULE_SYNTAX_ERROR;

public final class DroolsHelper {

    private static final Logger log = LoggerFactory.getLogger(DroolsHelper.class);


    /**
     * base on a ruleContent get PackageDescr
     *
     * @param ruleContent all rule content
     * @return an object that get all information from rule content
     */
    public PackageDescr parseDrl(@NonNull String ruleContent) {
        try {
            PackageDescr parsed = new DrlParser().parse(null, ruleContent);
            if (parsed == null) {
                throw new RuleException("Invalid DRL syntax", RULE_SYNTAX_ERROR);
            }
            return parsed;
        } catch (DroolsParserException e) {
            throw new RuleException("Invalid DRL syntax: " + e.getMessage(), RULE_SYNTAX_ERROR);
        }
    }

    /**
     * getting all global objects from rule content
     *
     * @param ruleContent all ruleContent
     * @return a map that it's key is name of global and value is type of global object
     */
    public Map<String, String> getGlobalsFromRuleContent(@NonNull String ruleContent) {
        PackageDescr packageDescr = parseDrl(ruleContent);
        validateDuplicateGlobalObjects(List.of(ruleContent));
        return packageDescr.getGlobals()
                .stream()
                .collect(Collectors.toMap(GlobalDescr::getIdentifier, GlobalDescr::getType));
    }

    /**
     * getting package of a rules from ruleContent
     *
     * @param ruleContent all rule Content
     * @return full package address as String
     */
    public String getPackageName(@NonNull String ruleContent) {
        PackageDescr packageDescr = parseDrl(ruleContent);
        return getPackageName(packageDescr);
    }

    /**
     * getting package of rule from PackageDescr
     *
     * @param packageDescr an object that extract from ruleContent
     * @return full package name as String
     */
    public String getPackageName(@NonNull PackageDescr packageDescr) {
        return packageDescr != null ? packageDescr.getName() : null;
    }

    /**
     * extract ruleName from ruleContent
     *
     * @param ruleContent all rule content
     * @return rule name that in rule content
     */
    public String getRuleName(@NonNull String ruleContent) {
        PackageDescr packageDescr = parseDrl(ruleContent);
        return getRuleName(packageDescr);
    }

    /**
     * extract rule name from packageDescr
     * must every rule content have only one rule
     *
     * @param packageDescr getting from rule content
     * @return ruleName
     */
    public String getRuleName(@NonNull PackageDescr packageDescr) {
        List<RuleDescr> ruleList = packageDescr.getRules();
        //validation exception check here:
        validateNoRulesFound(ruleList);
        validateMultipleRulesFound(ruleList);
        return ruleList.getFirst().getName();
    }

    /**
     * getting list of rule from list of path
     *
     * @param paths from resource folder
     * @return list all rule from all path
     * @throws RuleException if path does not include any rule or can not be read
     */
    public List<String> getRulesFromFiles(String... paths) throws RuleException {
        List<String> rules = new ArrayList<>();
        for (String resourcePath : paths) {
            rules.add(getContentFromResourcePath(resourcePath));
        }
        return rules;
    }

    /**
     * validate that in list of rule does not have same global name for different object type
     * that better for same scenario
     * if a globalName duplicate for different type @throws  RuleException
     *
     * @param ruleContentList list of rule
     */
    public void validateDuplicateGlobalObjects(@NonNull List<String> ruleContentList) {
        Map<String, HashSet<String>> globalObjects = new HashMap<>();
        ruleContentList.stream().flatMap(ruleContent -> {
            PackageDescr packageDescr = parseDrl(ruleContent);
            return packageDescr.getGlobals().stream();
        }).forEach(globalObject -> {
            HashSet<String> value = globalObjects.getOrDefault(globalObject.getIdentifier(), new HashSet<>());
            value.add(globalObject.getType());
            globalObjects.put(globalObject.getIdentifier(), value);
        });
        List<String> errors = new ArrayList<>();
        globalObjects.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> {
                    String message = String.format("global object %s has defined multiple with different types %s", entry.getKey(), entry.getValue());
                    log.error(message);
                    errors.add(message);
                });
        if (!errors.isEmpty())
            throw new RuleException("global objects problem:\n" + errors, RuleValidationCode.GLOBAL_VARIABLE_DUPLICATED_FOUND);
    }

    /**
     * validate that in list of rules no duplicate ruleName
     * if find duplicate @throws RuleException
     *
     * @param ruleContentList list of rule content
     */
    public void validateDuplicateRules(@NonNull List<String> ruleContentList) {
        List<String> ruleNameList = new ArrayList<>(ruleContentList.stream()
                .map(this::getRuleName)
                .toList());
        Set<String> seen = new HashSet<>();
        ruleNameList.removeIf(seen::add);
        if (ruleNameList.isEmpty()) return;
        ruleNameList.forEach(ruleName -> log.error("Duplicate rule name {}", ruleName));
        String message = String.format("duplicate rule names found %s", ruleNameList);
        throw new RuleException(message, RuleValidationCode.MULTIPLE_RULES_FOUND);
    }

    /**
     * getting rule content from resource files
     *
     * @param resourcePath from resource folder
     * @return all rule content
     * @throws RuleException if io have issue or can not read file
     */
    public String getContentFromResourcePath(@NonNull String resourcePath) throws RuleException {
        try (InputStream is = ResourceFactory.newClassPathResource(resourcePath).getInputStream()) {
            if (is == null) {
                log.error("no rules at this path {}", resourcePath);
                throw new RuleException(String.format("no rules at this path %s", resourcePath), RULE_NOT_FOUND);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("error reading at this path {}", resourcePath);
            throw new RuleException(String.format("error reading at this path %s", resourcePath), RULE_NOT_FOUND);
        }
    }

    /**
     * create a KieContainer for list of RuleItemEntity and validate rule before create kieContainer
     *
     * @param rules list of ruleItemEntity object
     * @return an instance of kieContainer base on param
     */
    public KieContainer buildKieContainer(@NonNull Set<RuleItemEntity> rules) {
        KieFileSystem kieFileSystem = getKieFileSystemFromStringRules(rules);
        return buildAndRegisterContainer(kieFileSystem);
    }


    /**
     * validate list of rule before create kieContainer
     * validate for duplicate rule name and duplicate globalName for different type
     *
     * @param ruleContentList list of rule content
     */
    public void validateRuleSet(List<String> ruleContentList) {
        validateDuplicateRules(ruleContentList);
        validateDuplicateGlobalObjects(ruleContentList);
    }


    private void validateMultipleRulesFound(List<RuleDescr> ruleList) {
        if (ruleList != null && ruleList.size() > 1) {
            log.error("multiple rules found");
            throw new RuleException("multiple rules found", RuleValidationCode.MULTIPLE_RULES_FOUND);
        }
    }

    private void validateNoRulesFound(List<RuleDescr> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            log.error("no rules found");
            throw new RuleException("no rules found", RuleValidationCode.RULE_NOT_FOUND);
        }
    }


    private KieFileSystem getKieFileSystemFromStringRules(Set<RuleItemEntity> rules) {
        try {
            KieServices ks = KieServices.Factory.get();
            KieFileSystem kfs = ks.newKieFileSystem();
            writeOnKieFileSystem(kfs, rules);
            validateRuleSet(rules.stream().map(RuleItemEntity::getRuleContent).collect(Collectors.toList()));
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

    private KieContainer buildAndRegisterContainer(KieFileSystem kfs) {
        KieServices ks = KieServices.Factory.get();
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


    private void writeOnKieFileSystem(KieFileSystem kfs, Set<RuleItemEntity> rules) {
        rules.forEach(ruleItemEntity -> {
            String ruleContent = ruleItemEntity.getRuleContent();
            PackageDescr packageDescr = parseDrl(ruleItemEntity.getRuleContent());
            String packName = getPackageName(packageDescr);
            String kiePath = String.format("src/main/resources/%s/%s.drl", (packName == null ? "" : packName.replace('.', '/').replace(";", "")), ruleItemEntity.getRuleName());
            kfs.write(kiePath, ruleContent);
            log.info("Adding DRL content for rule {}", ruleItemEntity.getRuleName());
        });
    }

    public FactDto toFactDto(Object object) {
        return toFactDto(object, new HashSet<>());
    }

    private FactDto toFactDto(Object object, Set<Integer> visitedObjects) {
        if (object == null) {
            return null;
        }

        // Check for circular reference using identity hash
        int objectId = System.identityHashCode(object);
        if (visitedObjects.contains(objectId)) {
            // Object already visited, return a simplified version to break the cycle
            FactDto circularRef = new FactDto();
            circularRef.setName(object.getClass().getSimpleName());
            circularRef.setType(object.getClass().getSimpleName() + " (circular reference)");
            circularRef.setValue(null);
            return circularRef;
        }

        // Add current object to visited set
        visitedObjects.add(objectId);

        FactDto factDto = new FactDto();
        Class<?> clazz = object.getClass();

        factDto.setName(clazz.getSimpleName());
        factDto.setType(clazz.getSimpleName());

        List<FactDto> fieldFactDtos = new ArrayList<>();
        Field[] fields = FieldUtils.getAllFields(clazz);

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object fieldValue = field.get(object);
                FactDto fieldFactDto = convertFieldToFactDto(field.getName(), field.getType(), fieldValue, visitedObjects);
                if (fieldFactDto != null) {
                    fieldFactDtos.add(fieldFactDto);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        factDto.setValue(fieldFactDtos);

        // Remove from visited set when done (backtracking)
        // This allows the same object to appear in different branches
        visitedObjects.remove(objectId);

        return factDto;
    }

    private FactDto convertFieldToFactDto(String fieldName, Class<?> fieldType, Object fieldValue, Set<Integer> visitedObjects) {
        if (fieldValue == null) {
            return null;
        }

        FactDto fieldFactDto = new FactDto();
        fieldFactDto.setName(fieldName);
        fieldFactDto.setType(getTypeName(fieldType));

        // Check if it's a primitive type or String
        if (isNotFactDto(fieldType) || fieldType == String.class) {
            if (isEnum(fieldType)) {
                fieldFactDto.setType("String");
                fieldFactDto.setValue(fieldValue.toString());
            }
            if (Date.class.isAssignableFrom(fieldType)) {
                Date date = (Date) fieldValue;
                long timeStamp = date.toInstant().toEpochMilli();
                fieldFactDto.setValue(timeStamp);
            }else if (fieldValue instanceof TemporalAccessor temporalAccessor) {
                long timeStamp = getUtcTimestamp(temporalAccessor);
                fieldFactDto.setValue(timeStamp);
            }else fieldFactDto.setValue(fieldValue);
        }
        // Check if it's a Collection
        else if (Collection.class.isAssignableFrom(fieldType)) {
            List<FactDto> collectionFactDtos = new ArrayList<>();
            Collection<?> collection = (Collection<?>) fieldValue;

            for (Object item : collection) {
                if (item != null) {
                    // Check for circular reference in collection item
                    int itemId = System.identityHashCode(item);
                    if (visitedObjects.contains(itemId)) {
                        FactDto circularRef = new FactDto();
                        circularRef.setName("");
                        circularRef.setType(item.getClass().getSimpleName() + " (circular reference)");
                        circularRef.setValue(null);
                        collectionFactDtos.add(circularRef);
                        continue;
                    }

                    FactDto itemFactDto = new FactDto();
                    itemFactDto.setName("");
                    itemFactDto.setType(item.getClass().getSimpleName());

                    // If item is a complex object, convert its fields
                    if (!isPrimitiveOrWrapper(item.getClass()) && item.getClass() != String.class) {
                        visitedObjects.add(itemId);

                        List<FactDto> itemFields = new ArrayList<>();

                        Field[] itemFieldsArray = FieldUtils.getAllFields(item.getClass());

                        for (Field itemField : itemFieldsArray) {
                            itemField.setAccessible(true);
                            try {
                                Object itemFieldValue = itemField.get(item);
                                FactDto itemFieldFactDto = convertFieldToFactDto(
                                        itemField.getName(),
                                        itemField.getType(),
                                        itemFieldValue,
                                        visitedObjects
                                );
                                if (itemFieldFactDto != null) {
                                    itemFields.add(itemFieldFactDto);
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        itemFactDto.setValue(itemFields);
                        visitedObjects.remove(itemId);
                    } else {
                        itemFactDto.setValue(item);
                    }

                    collectionFactDtos.add(itemFactDto);
                }
            }
            fieldFactDto.setValue(collectionFactDtos);
        }
        // Check if it's a complex object
        else {
            // Check for circular reference
            int fieldValueId = System.identityHashCode(fieldValue);
            if (visitedObjects.contains(fieldValueId)) {
                fieldFactDto.setType(fieldType.getSimpleName() + " (circular reference)");
                fieldFactDto.setValue(null);
                return fieldFactDto;
            }

            visitedObjects.add(fieldValueId);

            List<FactDto> nestedFields = new ArrayList<>();
            Field[] nestedFieldsArray = FieldUtils.getAllFields(fieldType);

            for (Field nestedField : nestedFieldsArray) {
                nestedField.setAccessible(true);
                try {
                    Object nestedFieldValue = nestedField.get(fieldValue);
                    FactDto nestedFieldFactDto = convertFieldToFactDto(
                            nestedField.getName(),
                            nestedField.getType(),
                            nestedFieldValue,
                            visitedObjects
                    );
                    if (nestedFieldFactDto != null) {
                        nestedFields.add(nestedFieldFactDto);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            fieldFactDto.setValue(nestedFields);
            visitedObjects.remove(fieldValueId);
        }

        return fieldFactDto;
    }

    private String getTypeName(Class<?> type) {
        // For primitives and their wrappers, use full name
        if (isPrimitiveOrWrapper(type)) {
            return type.getName();
        }

        // For String, use simple name "String"
        if (type == String.class) {
            return "String";
        }

        // For Java standard library types (java.*, javax.*), use full name
        if (isJavaType(type)) {
            return type.getName();
        }

        // For custom types, use simple name
        return type.getSimpleName();
    }

    private boolean isJavaType(Class<?> type) {
        String packageName = type.getPackage() != null ? type.getPackage().getName() : "";
        return packageName.startsWith("java.") || packageName.startsWith("javax.");
    }

    private boolean isNotFactDto(Class<?> clazz) {
        return isPrimitiveOrWrapper(clazz) || isDateTime(clazz) || isEnum(clazz);
    }

    private boolean isEnum(Class<?> clazz ) {
        return clazz.isEnum() || (clazz.getSuperclass() != null && clazz.getSuperclass().isEnum());
    }
    private boolean isDateTime(Class<?> clazz) {
        return java.util.Date.class.isAssignableFrom(clazz) ||
                        java.sql.Date.class.isAssignableFrom(clazz) ||
                        java.time.LocalDateTime.class.isAssignableFrom(clazz) ||
                        java.time.LocalDate.class.isAssignableFrom(clazz) ||
                        java.time.LocalTime.class.isAssignableFrom(clazz) ||
                        java.time.ZonedDateTime.class.isAssignableFrom(clazz) ||
                        java.time.Instant.class.isAssignableFrom(clazz);
    }

    public long getUtcTimestamp(TemporalAccessor temporal) {
        if (temporal instanceof Instant) {
            return ((Instant) temporal).toEpochMilli();
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toInstant().toEpochMilli();
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        }else if (temporal instanceof LocalTime) {
            return ((LocalTime) temporal).toEpochSecond(LocalDate.now(), ZoneOffset.UTC);
        }
        throw new UnsupportedOperationException("Unsupported type");
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive()
                || type == Boolean.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class;
    }

}
