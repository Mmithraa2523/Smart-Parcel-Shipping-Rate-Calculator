package com.swiftship.shipping.api;

import com.swiftship.shipping.infrastructure.SpringDataBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShippingApiIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private SpringDataBookingRepository bookings;

    private final HttpClient client = HttpClient.newHttpClient();
    private String api;

    @BeforeEach
    @Transactional
    void resetBookings() {
        bookings.deleteAll();
        api = "http://127.0.0.1:" + port + "/api";
    }

    @Test
    void exercisesQuotesValidationBusinessRejectionsAndPaymentBackedBookings() throws IOException, InterruptedException {
        HttpResponse<String> tiers = get("/tiers");
        assertEquals(200, tiers.statusCode(), tiers.body());
        assertTrue(tiers.body().contains("\"STANDARD\""));
        assertTrue(tiers.body().contains("\"EXPRESS\""));
        assertTrue(tiers.body().contains("\"SAME_DAY\""));

        assertQuote("/quotes", """
                {"weightKg":2.5,"distanceKm":100,"tier":"STANDARD"}
                """, 200, "\"total\":13.00");
        assertQuote("/quotes", """
                {"weightKg":2.5,"distanceKm":100,"tier":"EXPRESS"}
                """, 200, "\"total\":33.25");
        assertQuote("/quotes", """
                {"weightKg":2.5,"distanceKm":100,"tier":"SAME_DAY"}
                """, 200, "\"total\":70.00");
        HttpResponse<String> minimum = quote("""
                {"weightKg":0.1,"distanceKm":1,"tier":"STANDARD"}
                """);
        assertEquals(200, minimum.statusCode(), minimum.body());
        HttpResponse<String> maximum = quote("""
                {"weightKg":50,"distanceKm":2500,"tier":"STANDARD"}
                """);
        assertEquals(200, maximum.statusCode(), maximum.body());

        HttpResponse<String> invalidWeight = quote("""
                {"weightKg":50.1,"distanceKm":1,"tier":"STANDARD"}
                """);
        assertEquals(400, invalidWeight.statusCode(), invalidWeight.body());
        assertTrue(invalidWeight.body().contains("VALIDATION_ERROR"));
        HttpResponse<String> invalidDistance = quote("""
                {"weightKg":1,"distanceKm":2501,"tier":"STANDARD"}
                """);
        assertEquals(400, invalidDistance.statusCode(), invalidDistance.body());
        HttpResponse<String> missingFields = quote("""
                {"weightKg":null,"distanceKm":null,"tier":""}
                """);
        assertEquals(400, missingFields.statusCode(), missingFields.body());
        assertTrue(missingFields.body().contains("\"details\""));

        HttpResponse<String> overLimitSameDay = quote("""
                {"weightKg":1,"distanceKm":101,"tier":"SAME_DAY"}
                """);
        assertEquals(422, overLimitSameDay.statusCode(), overLimitSameDay.body());
        assertTrue(overLimitSameDay.body().contains("SAME_DAY_DISTANCE_EXCEEDED"));
        HttpResponse<String> unsupportedTier = quote("""
                {"weightKg":1,"distanceKm":1,"tier":"OVERNIGHT"}
                """);
        assertEquals(422, unsupportedTier.statusCode(), unsupportedTier.body());
        assertTrue(unsupportedTier.body().contains("UNSUPPORTED_TIER"));

        HttpResponse<String> unsupportedPayment = post("/bookings", """
                {"weightKg":1,"distanceKm":1,"tier":"STANDARD","paymentMethod":"CRYPTO"}
                """);
        assertEquals(422, unsupportedPayment.statusCode(), unsupportedPayment.body());

        HttpResponse<String> confirmed = post("/bookings", """
                {"weightKg":2.5,"distanceKm":100,"tier":"EXPRESS","paymentMethod":"CREDIT_CARD"}
                """);
        assertEquals(201, confirmed.statusCode(), confirmed.body());
        assertTrue(confirmed.body().contains("\"status\":\"CONFIRMED\""));
        assertTrue(confirmed.body().contains("\"paymentReference\":\"SIM-"));
        String bookingId = confirmed.body().split("\"bookingId\":\"")[1].split("\"")[0];

        HttpResponse<String> persisted = get("/bookings/" + bookingId);
        assertEquals(200, persisted.statusCode(), persisted.body());
        assertTrue(persisted.body().contains("\"total\":33.25"));
        assertTrue(persisted.body().contains("\"paymentMethod\":\"CREDIT_CARD\""));

        HttpResponse<String> failure = post("/bookings", """
                {"weightKg":1,"distanceKm":1,"tier":"STANDARD","paymentMethod":"CORPORATE_ACCOUNT_BILLING","simulatePaymentFailure":true}
                """);
        assertEquals(402, failure.statusCode(), failure.body());
        assertTrue(failure.body().contains("PAYMENT_DECLINED"));
        assertEquals(1, bookings.count(), "Only the successful booking should be persisted.");

        HttpResponse<String> missingBooking = get("/bookings/does-not-exist");
        assertEquals(404, missingBooking.statusCode(), missingBooking.body());
        assertTrue(missingBooking.body().contains("BOOKING_NOT_FOUND"));
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(api + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> quote(String body) throws IOException, InterruptedException {
        return post("/quotes", body);
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(api + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void assertQuote(String path, String request, int status, String expectedBody) throws IOException, InterruptedException {
        HttpResponse<String> response = post(path, request);
        assertEquals(status, response.statusCode(), response.body());
        assertTrue(response.body().contains(expectedBody), response.body());
    }
}
