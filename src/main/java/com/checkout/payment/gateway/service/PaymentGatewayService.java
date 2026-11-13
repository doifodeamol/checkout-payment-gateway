package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.BankPaymentRequest;
import com.checkout.payment.gateway.model.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {
  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient bankClient;
  private final PaymentValidator paymentValidator;

  public PaymentGatewayService(PaymentsRepository paymentsRepository,
      AcquiringBankClient bankClient, PaymentValidator paymentValidator) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
    this.paymentValidator = paymentValidator;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Retrieving payment {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new EventProcessingException("Payment not found"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    paymentValidator.validate(request); // Invalid requests never reach the bank.

    BankPaymentRequest bankRequest = new BankPaymentRequest(
        request.getCardNumber(), request.getExpiryDate(), request.getCurrency().toUpperCase(Locale.ROOT),
        request.getAmount(), request.getCvv());
    BankPaymentResponse bankResponse = bankClient.authorize(bankRequest);
    if (bankResponse == null || bankResponse.getAuthorized() == null) {
      throw new com.checkout.payment.gateway.exception.BankUnavailableException(
          "Acquiring bank returned an incomplete response", null);
    }

    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(bankResponse.getAuthorized()
        ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED);
    String cardNumber = request.getCardNumber();
    response.setCardNumberLastFour(cardNumber.substring(cardNumber.length() - 4));
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency().toUpperCase(Locale.ROOT));
    response.setAmount(request.getAmount());

    paymentsRepository.add(response);
    LOG.info("Payment {} processed with status {}", response.getId(), response.getStatus());
    return response;
  }
}
