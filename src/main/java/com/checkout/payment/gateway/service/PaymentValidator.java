package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.time.YearMonth;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PaymentValidator {
  // Deliberately limited to three currencies as required by the exercise.
  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("GBP", "USD", "EUR");

  public void validate(PostPaymentRequest request) {
    if (request == null) {
      throw new InvalidPaymentRequestException("Request body is required");
    }
    String cardNumber = request.getCardNumber();
    if (cardNumber == null || !cardNumber.matches("[0-9]{14,19}")) {
      throw new InvalidPaymentRequestException("Card number must contain 14 to 19 numeric digits");
    }
    Integer month = request.getExpiryMonth();
    Integer year = request.getExpiryYear();
    if (month == null || month < 1 || month > 12 || year == null || year < 1000 || year > 9999) {
      throw new InvalidPaymentRequestException("A valid expiry month and year are required");
    }
    YearMonth expiry = YearMonth.of(year, month);
    if (expiry.isBefore(YearMonth.now())) {
      throw new InvalidPaymentRequestException("Card has expired");
    }
    if (request.getCurrency() == null ||
        !SUPPORTED_CURRENCIES.contains(request.getCurrency().toUpperCase(Locale.ROOT))) {
      throw new InvalidPaymentRequestException("Currency must be one of GBP, USD or EUR");
    }
    if (request.getAmount() == null || request.getAmount() <= 0) {
      throw new InvalidPaymentRequestException("Amount must be a positive integer in minor units");
    }
    String cvv = request.getCvv();
    if (cvv == null || !cvv.matches("[0-9]{3,4}")) {
      throw new InvalidPaymentRequestException("CVV must contain 3 or 4 numeric digits");
    }
  }
}
