# Project Instructions

## Requirements

- Treat `capstone-smart-shipping-3hr.md` as the source of truth. Keep payment simulated and expose only the three required production shipping tiers.
- Preserve Java 21, Spring Boot 3.4+, Apache CXF JAX-RS with `JAXRSServerFactoryBean`, Spring Data JPA, and H2. Do not use Spring MVC `@RestController`.
- Preserve the specified price formulas, input limits, and predictable Same-Day distance rejection. Use `BigDecimal` for money.
- Use standalone Angular components and Signals, with the replaceable API adapter provided through `SHIPPING_API`.
- Keep the Node.js 26 host and `/api` proxy aligned with the backend port.

## Architecture

- Keep HTTP handling, pricing, booking orchestration, payment, persistence, and UI rendering in separate responsibilities.
- Inject backend abstractions through constructors; do not instantiate infrastructure dependencies in business logic.
- Keep pricing extensible without tier-specific coordinator branches and keep tier presentation data-driven.
- Update `openapi.yaml` when an API request or response contract changes.

## Validation

- Backend, from `backend`: `.\mvnw.cmd clean verify`
- Frontend, from `frontend`: `npm ci`, `npm run test:all`, and `npm run build`
- If port 8080 is occupied, run the backend with `SERVER_PORT=8081` and configure the Node host's `API_BACKEND_URL` to `http://127.0.0.1:8081`.
- Report only builds, tests, API checks, and runtime flows that were actually run and observed.
