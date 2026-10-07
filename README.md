# rule-sdk 1.0.0

Java 21 library for executing Drools rules locally or through an independent ruleGate service. Spring Boot integration is optional. The existing `RuleEngineServiceContext` and `DroolsRepository` APIs remain available; `RuleExecutor` provides one execution interface for applications that select local or remote mode.

## Install

```xml
<dependency>
    <groupId>io.github.amirhosseinfsh</groupId>
    <artifactId>rule-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

Install or publish this artifact before referencing it from another project. For Spring Boot property configuration, the consuming application must supply compatible Spring Boot classes.

## Configure execution

For local DRL files under `src/main/resources/rules/<scenario-name>/*.drl`:

```properties
rules.execution.mode=local
rules.execution.local.rule-dir=rules
rules.execution.local.auto-load=true
rules.execution.local.strict-validation-metadata=false
```

The strict setting is `false` by default. When set to `true`, every local rule must declare a non-empty `@MESSAGE` and `@ERROR(true)` or `@ERROR(false)` before the ruleset is compiled. The same check applies to local rule updates. If the application supplies its own `LocalRuleEngineService` bean, construct it with strict checking enabled as well.

For remote execution through ruleGate:

```properties
rules.execution.mode=remote
rules.execution.remote.base-url=http://localhost:8080
rules.execution.remote.connect-timeout=3s
rules.execution.remote.request-timeout=10s
```

The remote service owns and compiles its rules; this SDK sends facts to `POST /api/drools/execute` and reads its response. Remote DRL must use types available to that service. The local strict setting cannot validate rules stored in the remote service. If `rules.execution.mode` is absent, no `RuleExecutor` is auto-configured.

## Local rules and results

Local rules are ordinary DRL. Annotations are optional when strict checking is off:

```drl
package sample

rule "eligible"
    @MESSAGE("Applicant is eligible")
    @ERROR(false)
when
    String(this == "ready")
then
end
```

The `then` keyword is still required by DRL, but its body can be empty. If the body contains actions, those actions execute. Local rules can also use globals and agenda groups.

The local executor returns one `RuleValidation` per fired rule. `ruleName` is always populated; `message` is `null` without `@MESSAGE`, and `type` is `null` without `@ERROR`. `@ERROR(true)` maps to `ERROR`; `@ERROR(false)` maps to `WARNING`. A rule with no annotations still appears in the result with both optional fields `null`.

```java
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.local("configured", List.of("ready"), Map.of(), null));

for (RuleValidation validation : result.validations()) {
    System.out.println(validation.ruleName() + ": " + validation.message());
}
```

To collect your own output from `then`, declare a DRL global and pass the same mutable object in the local request:

```java
List<String> collected = new ArrayList<>();
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.local("my-scenario", List.of(fact),
        Map.of("collected", collected), "my-agenda-group"));
// Rules may add values to collected while also producing RuleValidation entries.
```

The supplied global names must match those declared in the local ruleset. The last request argument selects an agenda group; use `null` for the default agenda. If you construct `LocalRuleEngineService` manually, its three-argument constructor accepts `strictValidationMetadata` as the last argument.

## Remote results

Select the remote scenario on each call:

```java
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.remote(186L, List.of(applicant)));
```

For an authenticated request, pass the caller's access token on that call:

```java
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.remote(186L, List.of(applicant)), accessToken);
```

The SDK sends the token as a Bearer header for that request. It maps `content.ruleValidations` from the remote response into `RuleValidation`; missing `ruleName`, `message`, or `type` fields become `null`. Remote requests do not support local globals or agenda groups.

If one call site must work with either configured mode, supply both scenario identifiers and omit local-only options:

```java
RuleExecutionRequest request = new RuleExecutionRequest(
    "my-scenario", 186L, List.of(applicant), Map.of(), null);
RuleExecutionResult result = ruleExecutor.execute(request);
```

## Test

Run `mvn test`. Remote tests use a local HTTP test server and do not need a running ruleGate service.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
