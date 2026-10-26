# Requirements and verification checklist

This matrix maps every material capstone requirement to the implementation and its verification.

## Project stack and architecture

| Requirement | Implementation | Verification |
| --- | --- | --- |
| Java 21 and Spring Boot 3.4+ | `backend/pom.xml` configures Spring Boot 3.4.13 and `java.version` 21. The Maven Wrapper is configured in `backend/.mvn/wrapper/maven-wrapper.properties`. | From `backend`, run `.\mvnw.cmd clean verify`; the Maven compiler uses `--release 21`. |
| Apache CXF JAX-RS using `cxf-spring-boot-starter-jaxrs` and `JAXRSServerFactoryBean`; no MVC controllers. | `backend/pom.xml`, `backend/src/main/java/com/swiftship/shipping/config/CxfConfiguration.java`, and the JAX-RS resource classes in `backend/src/main/java/com/swiftship/shipping/api/`. | `ShippingApiIntegrationTest` sends real HTTP requests to a randomly assigned Spring Boot port. |
| Spring Data JPA and H2 at `jdbc:h2:mem:shippingdb`. | `backend/src/main/resources/application.properties`; `BookingEntity`, `SpringDataBookingRepository`, and `JpaBookingStore`. | The HTTP integration test reads the saved booking and checks the JPA repository after both successful and declined payments. |
| OpenAPI 3.1 API contract. | Root `openapi.yaml`. | Parsed as OpenAPI 3.1; independently validates the documented quote and booking requests, rejects unsupported payment methods, and rejects extra booking properties. |
| Angular 20+ standalone components and Signals. | `frontend/package.json`, `frontend/src/app/app.component.ts`, and `frontend/src/app/shipping-page.component.ts`. Angular 22 is selected for its explicit Node 26 support. | From `frontend`, run `npm run test:all` and `npm run build`. |
| Angular `InjectionToken` and replaceable production/test adapters. | `SHIPPING_API`, `HttpShippingApi`, and `appConfig` in `frontend/src/app/shipping-api.ts` and `frontend/src/app/app.config.ts`. | The Angular tests inject independent fake `ShippingApi` implementations. |
| Node 26 host and `/api` proxy to backend port 8080. | `frontend/server.mjs`, `frontend/package.json`, and `frontend/package-lock.json` (after npm install). | `frontend/server.test.mjs` checks SPA serving and forwards a real POST through the proxy to a local mock backend. |

## Business rules and API behavior

| Requirement | Implementation | Verification |
| --- | --- | --- |
| STANDARD formula: $5.00 + $1.20/kg + $0.05/km. | `StandardRateStrategy` in `backend/src/main/java/com/swiftship/shipping/domain/DefaultShippingRateStrategies.java`. | `FixedRateStrategyTest` and `ShippingApiIntegrationTest` assert $13.00 for 2.5 kg and 100 km. |
| EXPRESS formula: $15.00 + $2.50/kg + $0.12/km. | `ExpressRateStrategy` in `backend/src/main/java/com/swiftship/shipping/domain/DefaultShippingRateStrategies.java`. | The pricing unit and HTTP integration tests assert $33.25 for 2.5 kg and 100 km. |
| SAME_DAY formula: $35.00 + $4.00/kg + $0.25/km. | `SameDayRateStrategy` in `backend/src/main/java/com/swiftship/shipping/domain/DefaultShippingRateStrategies.java`. | The pricing unit and HTTP integration tests assert $70.00 for 2.5 kg at 100 km. |
| Decimal monetary calculations, minimum/maximum parcel weight, and minimum/maximum distance. | `BigDecimal`, `RoundingMode.HALF_UP`, and Jakarta validation constraints on `ApiModels.QuoteRequest` and `ApiModels.BookingRequest`. | `ShippingValidationTest` and `ShippingApiIntegrationTest` cover valid boundaries and invalid inputs. |
| Same-Day distances above 100 km receive an explicit business rejection. | The Same-Day rate strategy throws a typed `ShippingException`; `ShippingExceptionMapper` returns an error code and HTTP 422. | The pricing test and HTTP integration test assert `SAME_DAY_DISTANCE_EXCEEDED`. |
| Unsupported tiers and payment methods are rejected clearly. | `ShippingPricingService`, `ShippingBookingService`, and `ShippingExceptionMapper`. | Unit tests and the HTTP integration test verify the structured error and status. |
| List tiers and calculate itemized quotes. | Separate read-only `TierResource` and `QuoteResource`. | HTTP integration test checks all three tiers, every formula, boundaries, and validation errors. |
| Book only after simulated approval and persist through Spring Data JPA. | `ShippingBookingService`, `PaymentProcessor`, `JpaBookingStore`, and `BookingEntity`. | HTTP integration test confirms the booking is retrievable and that declined payment does not increase the saved-booking count. |
| Support both simulated success and failure without a real payment provider. | `SimulatedPaymentProcessor` and `simulatePaymentFailure` request flag. | Booking unit and HTTP integration tests verify both outcomes. |
| Functional responsive quote-and-book UI with clear errors. | `frontend/src/app/shipping-page.component.ts`, `.html`, and `.css`. | Component tests cover validation, quote rendering, booking success, and a declined-payment error. |

## SOLID acceptance criteria

| Principle | Backend demonstration | Frontend demonstration |
| --- | --- | --- |
| SRP | HTTP resources, `ShippingPricingService`, `ShippingBookingService`, `ShippingBookingQueryService`, `SimulatedPaymentProcessor`, and `JpaBookingStore` have distinct responsibilities. | `ShippingPageComponent` renders signals and validates user input; `HttpShippingApi` handles HTTP separately; `server.mjs` serves assets and proxies API requests. |
| OCP | `ShippingPricingService` discovers `ShippingRateStrategy` beans by their extension-provided `code()`. A new strategy is registered without adding a tier-specific coordinator branch. | Tier cards are rendered from `GET /tiers` metadata with a data-driven `@for` loop, not individual tier branches. |
| LSP | `ShippingRateStrategy` and `PaymentProcessor` share stable contracts. Delivery-limit violations return a typed business rejection instead of escaping as an unhandled exception. | `ShippingApi` is the stable adapter contract; normal request cleanup uses RxJS teardown and signal updates handle empty, loading, and failure results. |
| ISP | Read-only tier, quote, and booking lookup resources are distinct from the booking mutation resource; consumers depend on focused booking, payment, and pricing ports. | `Tier`, `Quote`, and `Booking` are display-specific transport models. UI rendering depends on the narrow `ShippingApi` contract. |
| DIP | Booking orchestration injects `ShippingQuoteCalculator`, `PaymentProcessor`, and `BookingStore` abstractions; the pricing service discovers `ShippingRateStrategy` implementations through constructor injection. | `SHIPPING_API` is an Angular `InjectionToken` configured to use `HttpShippingApi` in production and fake adapters in component tests. |

## Focused tests

- Backend: `backend/src/test/java/com/swiftship/shipping/domain/FixedRateStrategyTest.java`, `backend/src/test/java/com/swiftship/shipping/application/ShippingBookingServiceTest.java`, `backend/src/test/java/com/swiftship/shipping/application/ShippingValidationTest.java`, and `backend/src/test/java/com/swiftship/shipping/api/ShippingApiIntegrationTest.java`.
- Frontend component: `frontend/src/app/shipping-page.component.spec.ts`.
- Node host and API proxy: `frontend/server.test.mjs`.
