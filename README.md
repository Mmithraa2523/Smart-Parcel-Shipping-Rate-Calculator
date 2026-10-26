# SwiftShip smart parcel shipping

A complete quote-and-booking application for SwiftShip Logistics. It calculates itemized shipping rates, simulates card or corporate-account payments, and persists only approved bookings. There is no real payment provider.

## Prerequisites

- Java 21 LTS. The backend compiles against the Java 21 API (`--release 21`).
- The checked-in Maven Wrapper (`backend/mvnw.cmd`) downloads Maven 3.9.11; a separate Maven installation is not required.
- Node.js 26.x and npm 10 or newer. Run `node --version` and `npm --version` to check.
- Default API and frontend ports: backend **8080**, Node host **3000**. These ports must be available to their respective servers. If using Angular's development server, its port is **4200**.

The backend compiles against Java 21 (`--release 21`), but run it with an installed Java 21 runtime. In the current audit environment, Java 26.0.2 is installed and Java 21 is not; backend build, tests, and runtime checks therefore do not certify execution on Java 21. WinGet is also unavailable here, so the install command below was not run. Node 26.12.1 and Maven 3.9.11 were used for verification. Port 8080 is occupied by an existing `httpd.exe`; it was left untouched, and live application checks used port 8081 with the Node proxy configured to match.

### Install and select Java 21 on Windows

In PowerShell with WinGet available, install the Eclipse Temurin 21 JDK:

```powershell
winget install --exact --id EclipseAdoptium.Temurin.21.JDK
```

If WinGet is unavailable, download the Windows x64 JDK 21 MSI from [Eclipse Temurin releases](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jdk) and run the installer.

Open a new PowerShell terminal, locate the JDK, and explicitly select it for that terminal. Adjust the search path if you chose a custom install location:

```powershell
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" |
  Sort-Object Name -Descending |
  Select-Object -First 1
if (-not $jdk) { throw "Java 21 was not found under C:\Program Files\Eclipse Adoptium" }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
Set-Location E:\smart-shipping\backend
.\mvnw.cmd -version
```

Both version commands must report Java 21 before running the backend tests or application. The wrapper also reports the JVM it selected. The first wrapper invocation downloads Maven, so network access to Maven Central is required.

On Windows, if Node.js is installed but `node` or `npm` is not recognized, add the standard installation directory to the current PowerShell session and recheck:

```powershell
$env:Path = "C:\Program Files\nodejs;$env:Path"
node --version
npm --version
```

## Start the application

In PowerShell, open two terminals in the project directory.

**Terminal 1 — Spring Boot, Apache CXF JAX-RS, and H2**

```powershell
Set-Location E:\smart-shipping\backend
$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" |
  Sort-Object Name -Descending |
  Select-Object -First 1
if (-not $jdk) { throw "Install Java 21 or update this path to your JDK 21 installation." }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

The JDK search assumes the standard Eclipse Adoptium install directory; set `JAVA_HOME` directly if your JDK is elsewhere. The example uses port 8081 because 8080 may already be occupied; omit `SERVER_PORT` to use the configured default 8080. The API listens on `http://localhost:8081/api` with this example. H2 uses `jdbc:h2:mem:shippingdb`; it retains data while the backend process is running and is recreated at restart.

**Terminal 2 — install frontend dependencies, compile the SPA, and serve it with the Node 26 host**

```powershell
Set-Location E:\smart-shipping\frontend
$env:PORT = "3000"
$env:API_BACKEND_URL = "http://127.0.0.1:8081"
npm ci
npm run build
npm run host
```

Open `http://localhost:3000`. The Node server hosts the built Angular application and reverse-proxies `/api` requests to `http://127.0.0.1:8081` without CORS configuration.

For a development session instead, start the backend and then run:

```powershell
Set-Location frontend
npm start
```

Open `http://localhost:4200`; the Angular development server proxy defaults to backend port 8080 through `frontend/proxy.conf.json`. For a backend on port 8081, update the proxy target to match before starting `npm start`.

The backend defaults to port 8080 (`server.port=8080`). For testing, override Spring Boot's port with `SERVER_PORT`, and configure the Node host to proxy to that same address. You may also override the Node host's listening port with `PORT`. For example, start the backend in one terminal:

```powershell
Set-Location E:\smart-shipping\backend
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

Then start the Node host in another:

```powershell
Set-Location E:\smart-shipping\frontend
$env:PORT = "3001"
$env:API_BACKEND_URL = "http://127.0.0.1:8081"
npm run host
```

The Angular development-server proxy configuration targets the default backend port 8080; `API_BACKEND_URL` configures the Node production host.

If port 8080 is occupied, identify the listener without stopping it:

```powershell
$listener = Get-NetTCPConnection -State Listen -LocalPort 8080
$listener | ForEach-Object {
  Get-CimInstance Win32_Process -Filter "ProcessId=$($_.OwningProcess)" |
    Select-Object ProcessId, Name, CommandLine
}
```

Only stop a process after confirming with its owner that it is safe. Alternatively, leave it running and use `SERVER_PORT=8081` plus `API_BACKEND_URL=http://127.0.0.1:8081` as shown above.

## Delivery options and prices

| Code | Speed | Base | Per kilogram | Per kilometer | Additional limit |
| --- | --- | ---: | ---: | ---: | --- |
| `STANDARD` | 3–5 business days | $5.00 | $1.20 | $0.05 | None |
| `EXPRESS` | Next business day | $15.00 | $2.50 | $0.12 | None |
| `SAME_DAY` | Within 4 hours | $35.00 | $4.00 | $0.25 | 100 km |

**Required inputs:** weight `0.1`–`50.0` kg and distance `1`–`2500` km. Same-Day distances above 100 km produce a structured `422` business error. Amounts use `BigDecimal`, with each charge rounded to two decimal places using `HALF_UP`.

**Payment methods:** `CREDIT_CARD` and `CORPORATE_ACCOUNT_BILLING`. Payment is simulated; the application never requests a real card or account number. `simulatePaymentFailure: true` exercises a decline and does not persist a booking.

## API contract and examples

The OpenAPI 3.1 contract is in [`openapi.yaml`](./openapi.yaml). The following examples use the default backend port 8080; substitute 8081 when using the port override shown above.

**Calculate a quote**

```powershell
$quote = Invoke-RestMethod `
  -Uri http://localhost:8080/api/quotes `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"weightKg":2.5,"distanceKm":100,"tier":"STANDARD"}'
$quote
```

Response:

```json
{
  "tier": "STANDARD",
  "deliveryEstimate": "3-5 business days",
  "baseFee": 5.00,
  "weightCharge": 3.00,
  "distanceCharge": 5.00,
  "total": 13.00
}
```

**Book a shipment after simulated payment**

```powershell
$booking = Invoke-RestMethod `
  -Uri http://localhost:8080/api/bookings `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"weightKg":2.5,"distanceKm":100,"tier":"EXPRESS","paymentMethod":"CREDIT_CARD"}'
$booking
Invoke-RestMethod -Uri "http://localhost:8080/api/bookings/$($booking.bookingId)"
```

Creation returns `201` and a `Location` header. Only confirmed, approved bookings are stored.

**Exercise a simulated payment decline**

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8080/api/bookings `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"weightKg":1,"distanceKm":10,"tier":"STANDARD","paymentMethod":"CORPORATE_ACCOUNT_BILLING","simulatePaymentFailure":true}'
```

A decline responds with `402 PAYMENT_DECLINED`; no booking is written. Invalid weights or distances respond with `400 VALIDATION_ERROR` and individual field messages. Unknown tiers, payment methods, and Same-Day distances above 100 km respond with structured `422` errors.

**Read a saved booking:** `GET /api/bookings/{bookingId}`. A missing identifier responds with `404 BOOKING_NOT_FOUND`.

## Tests and production build

**Backend**, run from `backend` (Java 21 must be available through `JAVA_HOME`):

```powershell
.\mvnw.cmd clean verify
```

**Angular frontend and built-in Node proxy host**, run from `frontend`:

```powershell
npm ci
npm run test:all
npm run build
```

The backend tests verify all rate formulas, half-up rounding, inclusive parcel boundaries, invalid inputs, Same-Day rejection, tier extensibility, simulated payment outcomes, and real JAX-RS/HTTP requests backed by H2. Angular component tests verify form validation, displayed quotes, successful bookings, declined-payment feedback, and replaceable API adapters. Node's built-in test runner exercises the static host and a complete proxied HTTP request.

## Architecture and SOLID principles

**Backend:** Each Apache CXF JAX-RS resource handles HTTP input and output. `ShippingPricingService` discovers independent `ShippingRateStrategy` implementations and implements the injected `ShippingQuoteCalculator` contract. `ShippingBookingService` coordinates that abstraction with the `PaymentProcessor` and `BookingStore` contracts; `SimulatedPaymentProcessor` and `JpaBookingStore` implement those replaceable boundaries. The H2/JPA entity remains in infrastructure and is not an API model.

**Frontend:** `ShippingPageComponent` is a standalone Signals-driven UI using typed reactive forms. The separate `HttpShippingApi` implements the `ShippingApi` contract, injected via `SHIPPING_API`; unit tests replace that dependency without changing the component. The independent Node 26 `server.mjs` hosts Angular's production assets and streams `/api` requests to the backend.

- **SRP:** quote/tier HTTP handlers, booking orchestration, pricing, payment, persistence, frontend HTTP, and UI rendering have separate responsibilities.
- **OCP:** adding a backend tier means adding a rate strategy exposing a new `code()`, registering it as a Spring bean, and providing its strategy details. No tier-specific branch or coordinator change is required. `FixedRateStrategyTest` demonstrates a separately registered `OVERNIGHT` strategy in a test only; it is not an additional production tier. The frontend displays tier metadata returned from the API instead of hard-coding rate cards.
- **LSP:** implementations honor their `ShippingRateStrategy`, `PaymentProcessor`, `BookingStore`, and `ShippingApi` contracts. Same-Day delivery-limit violations are ordinary, typed business errors. Client adapter teardown and failure handling do not require consumers to special-case an implementation.
- **ISP:** quote/tier listing, quote calculation, booking creation, and booking lookup use separate resources and focused models; backend payment and persistence boundaries expose only the operations their callers need.
- **DIP:** application services receive backend pricing, payment, and persistence abstractions by constructor injection. Angular components receive the narrow `ShippingApi` contract through an `InjectionToken`, with production and test providers.

For a requirement-by-requirement implementation and verification mapping, see [`docs/requirements-checklist.md`](./docs/requirements-checklist.md).

## Implementation boundary

Authentication, carrier integrations, real payment processing, tracking, deployment automation, and production-grade observability are intentionally out of scope.
