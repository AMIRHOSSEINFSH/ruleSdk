# ruleSdk 2.0.0

`rule-sdk` executes DRL rules in a consumer application or sends serialized facts to ruleGate. Java 21 is required. The existing `RuleEngineServiceContext` and `DroolsRepository` local APIs remain available; the new `RuleExecutor` is the common entry point for applications that choose an execution mode through Spring Boot configuration.

## Add the dependency

```xml
<dependency>
    <groupId>ir.amirhosseinfsh</groupId>
    <artifactId>rule-sdk</artifactId>
    <version>2.0.0</version>
</dependency>
```

The version on this branch is a source version; publish or install the artifact before changing a consumer dependency. Spring Boot auto-configuration is optional. The consumer must provide Spring Boot 3.4 or compatible Boot classes for property-based mode selection.

## Choose a mode in `application.properties`

Local rules, loaded from `src/main/resources/rules/<scenario-name>/*.drl`:

```properties
rules.execution.mode=local
rules.execution.local.rule-dir=rules
rules.execution.local.auto-load=true
```

Remote rules, loaded by ruleGate from its database:

```properties
rules.execution.mode=remote
rules.execution.remote.base-url=http://localhost:8080
rules.execution.remote.connect-timeout=3s
rules.execution.remote.request-timeout=10s
```

No mode is selected when `rules.execution.mode` is absent, so existing applications keep their current wiring. In local mode, auto-configuration reuses an existing `LocalRuleEngineService` bean if one is registered; otherwise it creates an in-memory local repository and loads rules from the configured classpath directory. In remote mode, it creates an HTTP client for `POST /api/drools/execute`.

## Execute a scenario

Inject `RuleExecutor` into a Spring component. Select the scenario on **every call**. For example, the ruleGate scenario ID `186` belongs in the request, not in `application.properties`:

```java
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.remote(186L, List.of(applicant)));
for (RuleValidation finding : result.validations()) {
    System.out.println(finding.ruleName() + ": " + finding.message());
}
```

If ruleGate requires authentication, supply the current caller's raw access token on that execution call:

```java
String accessToken = tokenProvider.currentAccessToken();
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.remote(186L, List.of(applicant)), accessToken);
```

The SDK adds `Authorization: Bearer <accessToken>` to that HTTP request only. It does not keep the token in `application.properties` or in the executor instance. Pass `null` or use the one-argument `execute` method when no token is needed. Local mode ignores the token argument, allowing a shared call site to use either configured mode.

For a local rule that uses a `global java.util.List validations;` collector:

```java
List<BaseValidationDto> validations = new ArrayList<>();
RuleExecutionResult result = ruleExecutor.execute(
    RuleExecutionRequest.local("validateParvaneh", List.of(applicant),
        Map.of("validations", validations), null));
// The DRL may append detailed BaseValidationDto objects to validations.
```

When the same call site must work with either mode, pass both scenario identifiers in a `RuleExecutionRequest`:

```java
RuleExecutionRequest request = new RuleExecutionRequest(
    "validateParvaneh", 186L, List.of(applicant), Map.of(), null);
RuleExecutionResult result = ruleExecutor.execute(request);
```

The common result contains the scenario and rule findings. Local rules create findings through `@MESSAGE` and `@ERROR` rule metadata; local DRL globals still receive their original objects. Remote findings come from ruleGate's `content.ruleValidations` response. The remote `/execute` contract does **not** accept globals or an agenda group, so a request using either is rejected in remote mode. To run a rule remotely, store a compatible DRL rule and its `declare` types in ruleGate. It cannot import Java classes that exist only in the consumer.

A consumer such as `parvaresh` currently injects `RuleEngineServiceContext` and creates its own local service. To switch that application by property, migrate its execution call sites to `RuleExecutor` and adapt rules that depend on local globals or consumer Java classes. Merely changing the SDK dependency and the mode property will not redirect the old API to ruleGate.

## Manual use without Spring Boot

Construct `LocalRuleExecutor` with an existing `LocalRuleEngineService`, or construct `RemoteRuleExecutor` with a base URL and timeouts. Both implement `RuleExecutor`.

## Tests

Run `mvn test`. The tests execute local DRL rules and exercise remote HTTP requests against a local test server, including a different scenario ID on each call. They do not require a running ruleGate server.
