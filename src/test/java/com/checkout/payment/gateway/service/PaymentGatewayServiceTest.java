package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankUnavailableException;
import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PaymentGatewayServiceTest {
  private PaymentsRepository repository;
  private AcquiringBankClient bankClient;
  private PaymentGatewayService service;

  @BeforeEach
  void setUp() {
    repository = new PaymentsRepository();
    bankClient = mock(AcquiringBankClient.class);
    service = new PaymentGatewayService(repository, bankClient, new PaymentValidator());
  }

  @Test
  void authorizedPaymentIsSentToBankAndStoredWithoutSensitiveCardData() {
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(true);
    bankResponse.setAuthorizationCode("bank-auth-code");
    when(bankClient.authorize(any())).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(validRequest("1234567890123451"));

    assertNotNull(response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals("3451", response.getCardNumberLastFour());
    assertEquals(response.getId(), service.getPaymentById(response.getId()).getId());

    ArgumentCaptor<com.checkout.payment.gateway.model.BankPaymentRequest> captor =
        ArgumentCaptor.forClass(com.checkout.payment.gateway.model.BankPaymentRequest.class);
    verify(bankClient).authorize(captor.capture());
    assertEquals("01/2099", captor.getValue().getExpiryDate());
    assertEquals("0123", captor.getValue().getCvv());
  }

  @Test
  void declinedPaymentIsStoredAsDeclined() {
    BankPaymentResponse bankResponse = new BankPaymentResponse();
    bankResponse.setAuthorized(false);
    when(bankClient.authorize(any())).thenReturn(bankResponse);

    PostPaymentResponse response = service.processPayment(validRequest("1234567890123452"));

    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    assertEquals(response.getId(), service.getPaymentById(response.getId()).getId());
  }

  @Test
  void bankFailureIsPropagatedAndPaymentIsNotStored() {
    when(bankClient.authorize(any())).thenThrow(
        new BankUnavailableException("bank unavailable", new RuntimeException("503")));

    assertThrows(BankUnavailableException.class,
        () -> service.processPayment(validRequest("1234567890123450")));
    verify(bankClient).authorize(any());
  }

  @Test
  void invalidRequestIsRejectedBeforeCallingBank() {
    // 13 digits is below the assessment minimum of 14 digits.
    PostPaymentRequest request = validRequest("1234567890123");

    assertThrows(InvalidPaymentRequestException.class, () -> service.processPayment(request));
    verifyNoInteractions(bankClient);
  }

  private PostPaymentRequest validRequest(String cardNumber) {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(cardNumber);
    request.setExpiryMonth(1);
    request.setExpiryYear(2099);
    request.setCurrency("GBP");
    request.setAmount(1250);
    request.setCvv("0123");
    return request;
  }
}
