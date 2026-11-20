---
module: SPR-01
code: SPR-01
deliverable: 1
spec: v0.6.0
kind: deliverable
title: Build an expense submission API
subtitle: Record valid expense claims, refuse invalid ones with every reason, and apply each team's claim limit.
time_budget_min: 90
status: draft
---

## Topic: Deliverable

### Exercise: Build the expense submission API

A small team records employee expenses in a shared spreadsheet. Entries often arrive without a description, or claim more than the receipt total. Someone then has to chase the employee before the expense can be recorded.

Build a Spring Boot REST API that records valid expense submissions and lets the team retrieve them. The team needs to know who incurred each expense, what it was for, and how much is claimed. A claim cannot exceed its receipt total. Missing or malformed fields are all named in one response. Rejected submissions are not saved.

A claim can cover part of a receipt. An employee buys $120 of gasoline, but only $50 relates to a business trip. The receipt total is $120, and the claim is $50.

Each team runs its own copy of the API with its own maximum amount per claim. One team allows claims up to $500; another allows up to $100. The API applies the team's limit and explains when a claim exceeds it.

Operators need a health check to confirm the API is running. They also need request metrics: how many requests the API handled and how long they took. Actuator, Spring Boot's set of built-in monitoring endpoints, must not expose the application's settings over HTTP.

No starter is supplied. You generate the project yourself.

:::note
**Time limit:** 90 minutes.
:::

## Topic: Outcomes

### Theory: The finished work

- An employee can submit an expense, and the team can list submissions and retrieve one expense. An unknown expense identifier is reported as not found.
- Submissions with missing or malformed fields are refused without saving. One response names every failing field and explains what needs correcting.
- An employee may claim part or all of a receipt total, but not $80 against a $50 receipt. The refusal says why; nothing is saved.
- A $150 claim against a $150 receipt is accepted for a team whose limit is $500, and refused for a team whose limit is $100.
- Health reports `UP`, and request metrics are available after API traffic. Actuator endpoints other than health and metrics are unavailable over HTTP.

### Theory: Rules

- Use Java 25, Spring Boot 4, Gradle 9, Hibernate Validator 9, and JUnit 6.
- Before coding, write a short spec of the API contract, data model, validation rules, and acceptance checks. Build and verify against it.
- Keep expenses in memory. Amounts are greater than zero, in US dollars, with at most two decimal places.
- Out of scope: a user interface, authentication and Spring Security, receipt uploads, editing or deleting expenses, approval workflows, and payments.

### Theory: Your decisions

Endpoint paths, request and response shapes, and type and package names are yours. So is how a submission carries its receipt total; only the total matters, not the receipt itself.

You also choose which domain rule from your spec gets a custom constraint. A refusal for a broken rule, such as the team's limit, may come as its own response with the reason.

## Topic: Exit Criteria

### Theory: What must be in your submission

| ID | Capability | Evidence |
|---|---|---|
| D1-1 | Wire layered components with dependency injection | Beans use constructor injection; controllers hold no rules or data access, services no HTTP types; dependencies run controller, service, repository |
| D1-2 | Externalize configuration and switch it by profile | The submission limit binds through @ConfigurationProperties from a base file and a profile override, and activating the profile changes API behavior |
| D1-3 | Validate requests with Jakarta Bean Validation | Request validation uses built-in constraints and a custom constraint for a domain rule from the learner's spec; field failures return 400 naming every failing field |
| D1-4 | Expose health and metrics through Actuator | Only health and metrics Actuator endpoints are exposed over HTTP; health returns UP and HTTP request metrics reflect API traffic |
| D1-5 | Test the web layer in isolation | @WebMvcTest tests use MockMvc and a mocked service to assert status and JSON body for success and validation failure, including the custom constraint |
| D1-6 | Test the full application | A @SpringBootTest test on a random port submits and retrieves an expense over real HTTP through the real layers, and ./gradlew build passes on a clean clone |

### Exercise: What to submit

- The Gradle project, with its wrapper, source, and tests.
- Your spec.
- Passing test output.
- Requests and their responses showing each outcome, the profile switch, and the Actuator endpoints.
