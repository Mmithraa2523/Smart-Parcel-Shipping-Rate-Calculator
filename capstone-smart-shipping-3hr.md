# Capstone Project: Smart Parcel Shipping Rate Calculator (3--4 Hour Timebox)

## 1. Executive Summary & Context

You have been hired by SwiftShip Logistics to build a small, spec-driven
rate calculation and parcel booking engine. The system calculates
shipping costs based on parcel weight, distance, and delivery speed,
simulates payment, and saves the parcel booking.

This project is tailored specifically for a **3 to 4-hour hands-on
capstone**. The business domain is clean and minimal, yet every single
SOLID principle has a crisp, unambiguous mapping on both Backend and
Frontend.

## 2. Technical Stack Constraints

-   **Contract Specification:** Teams create a small OpenAPI 3.1
    contract from the requirements below. No API specification or
    generated code is provided.
-   **Backend:**
    -   Java 21 LTS & Spring Boot 3.4+.
    -   Apache CXF JAX-RS (`cxf-spring-boot-starter-jaxrs`) via
        `JAXRSServerFactoryBean`.
    -   No Spring MVC `@RestController` permitted.
    -   H2 In-Memory Database (`jdbc:h2:mem:shippingdb`) via Spring Data
        JPA.
-   **Frontend:**
    -   Angular 20+ Standalone Components with Signals.
    -   Angular `InjectionToken` for dependency inversion.
    -   Served by Node.js 26.x with `/api` requests proxied to the
        backend on port 8080.

### Greenfield Constraint

-   Developers receive this requirements document and an empty
    repository only.
-   No starter application, source files, OpenAPI contract, interfaces,
    tests, or intentional anti-pattern examples are provided.
-   Teams decide their own package structure, names, boundaries, and
    implementation details. The acceptance criteria below describe
    outcomes, not prescribed class designs.

## 3. Business Scenario & Rules

SwiftShip offers three shipping speed tiers:

-   **STANDARD** (3--5 business days): Base fee \$5.00 + \$1.20 per kg +
    \$0.05 per km.
-   **EXPRESS** (Next day): Base fee \$15.00 + \$2.50 per kg + \$0.12
    per km.
-   **SAME_DAY** (Within 4 hours): Base fee \$35.00 + \$4.00 per kg +
    \$0.25 per km.

### Delivery Rules

-   Weight must be between 0.1 kg and 50.0 kg.
-   Distance must be between 1 km and 2,500 km.
-   Same-Day delivery is restricted to distances ≤ 100 km.
-   Customers can pay via **Credit Card** or **Corporate Account
    Billing**.

## 4. SOLID Acceptance Criteria (The 5 Guardrails)

### Principle 1: Single Responsibility Principle (SRP)

-   **Backend Requirement:**
    -   Keep the JAX-RS resource focused on HTTP input/output and
        validation; separate rate calculation, booking orchestration,
        payment, and persistence responsibilities.
-   **Frontend Requirement:**
    -   Keep UI components focused on user interaction and rendering
        state. Place transport and business rules outside the component;
        avoid direct DOM manipulation.

### Principle 2: Open/Closed Principle (OCP)

-   **Backend Requirement:**
    -   Design rate calculation so a new shipping tier can be added
        without editing a central calculation coordinator or changing
        existing tier behavior.
    -   Demonstrate the extension point with a small test or written
        example; do not add extra tiers to the business requirements.
-   **Frontend Requirement:**
    -   Avoid scattering tier-specific conditionals through the UI. Keep
        tier presentation extensible and aligned with the backend
        contract.

### Principle 3: Liskov Substitution Principle (LSP)

-   **Backend Requirement:**
    -   Implementations of shared rate/payment contracts must preserve
        their documented preconditions and outcomes.
    -   A validly formed request for Same-Day delivery over 100 km must
        be represented as a predictable business rejection, not an
        unhandled runtime failure. The API should communicate the
        rejection clearly.
-   **Frontend Requirement:**
    -   Any interchangeable client-side adapter used by the application
        must honor its contract; normal cleanup or missing-value cases
        must not cause unexpected exceptions.

### Principle 4: Interface Segregation Principle (ISP)

-   **Backend Requirement:**
    -   Keep read-only quote/tier operations separate from booking
        mutations where clients have different needs.
    -   Avoid a single broad interface that forces consumers to depend
        on unrelated operations.
-   **Frontend Requirement:**
    -   Give presentational UI pieces only the data they display, rather
        than coupling them to persistence entities or oversized API
        models.

### Principle 5: Dependency Inversion Principle (DIP)

-   **Backend Requirement:**
    -   Business logic should depend on abstractions for persistence,
        payment, and rate calculation rather than concrete
        infrastructure implementations.
    -   Use constructor injection; avoid constructing infrastructure
        dependencies inside business logic.
-   **Frontend Requirement:**
    -   Components and facades should depend on replaceable service
        contracts, with production and test implementations configurable
        through Angular dependency injection.

## 5. Suggested 3--4 Hour Timebox Breakdown

  Activity                                                  Timebox
  ---------------------------------------------------- ------------
  1\. Derive API contract and sketch architecture        25 minutes
  2\. Implement backend quote and booking flow           75 minutes
  3\. Implement Angular quote and booking experience     65 minutes
  4\. Add focused tests, run, and review                 35 minutes

## 6. Greenfield Deliverables

Starting from an empty repository, teams build the smallest complete
vertical slice that satisfies the requirements:

1.  A short OpenAPI 3.1 contract for quote and booking operations.
2.  A Java 21 / Spring Boot backend exposing the API through Apache CXF
    JAX-RS, persisting bookings in H2, and returning clear
    validation/business errors.
3.  An Angular SPA to enter parcel details, choose a shipping tier, view
    a quote, and submit a booking.
4.  A Node.js 26.x host that serves the SPA and proxies `/api` to the
    backend.
5.  Focused tests for rate formulas, invalid inputs and Same-Day
    distance rejection, booking success/failure, and at least one
    replaceable dependency.
6.  A brief README with run instructions and a note explaining where the
    design demonstrates each SOLID principle.

Keep the scope intentionally small: payment providers are simulated;
authentication, real carrier integrations, tracking workflows,
deployment automation, and production-grade observability are out of
scope. Developers should not spend time creating deliberately bad code
to refactor: they must design and implement the solution from these
requirements.

## 7. Submission

Submit the application in a GitHub Pull Request. Include setup and run
instructions, the API contract, and tests for the main quote and booking
flows. Be prepared to explain the design decisions and demonstrate the
working application.
