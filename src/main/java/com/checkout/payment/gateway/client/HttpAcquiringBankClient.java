package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class HttpAcquiringBankClient implements AcquiringBankClient {
  private static final Logger LOG = LoggerFactory.getLogger(HttpAcquiringBankClient.class);
  private final RestTemplate restTemplate;
  private final String bankPaymentsUrl;

  public HttpAcquiringBankClient(RestTemplate restTemplate,
      @Value("${bank.base-url:http://localhost:8080}") String bankBaseUrl) {
    this.restTemplate = restTemplate;
    this.bankPaymentsUrl = bankBaseUrl.replaceAll("/$", "") + "/payments";
  }

  @Override
  public BankPaymentResponse authorize(BankPaymentRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    long startedAt = System.nanoTime();
    try {
      BankPaymentResponse response = restTemplate.postForObject(bankPaymentsUrl,
          new HttpEntity<>(request, headers), BankPaymentResponse.class);
      LOG.info("Acquiring bank responded in {} ms",
          (System.nanoTime() - startedAt) / 1_000_000);
      return response;
    } catch (RestClientException exception) {
      // Never log the request: it contains PAN and CVV.
      LOG.warn("Acquiring bank request failed: {}", exception.getClass().getSimpleName());
      throw new BankUnavailableException("Acquiring bank is currently unavailable", exception);
    }
  }
}
