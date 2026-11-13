package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpAcquiringBankClientTest {

  @Test
  void sendsExpectedBankPayloadAndMapsAuthorizationResponse() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(requestTo("http://localhost:8080/payments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().json("""
            {
              "card_number": "1234567890123451",
              "expiry_date": "12/2099",
              "currency": "GBP",
              "amount": 100,
              "cvv": "0123"
            }
            """))
        .andRespond(withSuccess(
            "{\"authorized\":true,\"authorization_code\":\"auth-123\"}",
            MediaType.APPLICATION_JSON));

    HttpAcquiringBankClient client = new HttpAcquiringBankClient(restTemplate,
        "http://localhost:8080");
    BankPaymentResponse response = client.authorize(
        new BankPaymentRequest("1234567890123451", "12/2099", "GBP", 100, "0123"));

    assertTrue(response.getAuthorized());
    assertEquals("auth-123", response.getAuthorizationCode());
    server.verify();
  }

  @Test
  void mapsBankServerErrorToBankUnavailable() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    server.expect(requestTo("http://localhost:8080/payments"))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    HttpAcquiringBankClient client = new HttpAcquiringBankClient(restTemplate,
        "http://localhost:8080");
    BankPaymentRequest request = new BankPaymentRequest(
        "1234567890123450", "12/2099", "GBP", 100, "123");

    assertThrows(BankUnavailableException.class, () -> client.authorize(request));
    server.verify();
  }
}
