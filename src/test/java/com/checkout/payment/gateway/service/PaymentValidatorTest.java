package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.checkout.payment.gateway.exception.InvalidPaymentRequestException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;

class PaymentValidatorTest {
  private final PaymentValidator validator = new PaymentValidator();

  @Test
  void acceptsValidPaymentRequest() {
    assertDoesNotThrow(() -> validator.validate(request("1234567890123451", "0123", "GBP", 100)));
  }

  @Test
  void rejectsCardNumberOutsideAllowedLength() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123", "123", "GBP", 100)));
  }

  @Test
  void rejectsNonNumericCardNumber() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("12345678901234AB", "123", "GBP", 100)));
  }

  @Test
  void rejectsInvalidCvv() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123451", "12A", "GBP", 100)));
  }

  @Test
  void rejectsUnsupportedCurrency() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123451", "123", "JPY", 100)));
  }

  @Test
  void rejectsNonPositiveAmount() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123451", "123", "GBP", 0)));
  }

  @Test
  void rejectsExpiredCard() {
    PostPaymentRequest request = request("1234567890123451", "123", "GBP", 100);
    request.setExpiryMonth(1);
    request.setExpiryYear(2000);
    assertThrows(InvalidPaymentRequestException.class, () -> validator.validate(request));
  }

  @Test
  void rejectsInvalidExpiryMonth() {
    PostPaymentRequest request = request("1234567890123451", "123", "GBP", 100);
    request.setExpiryMonth(13);
    assertThrows(InvalidPaymentRequestException.class, () -> validator.validate(request));
  }


  @Test
  void acceptsMinimumAndMaximumCardNumberLengths() {
    assertDoesNotThrow(() -> validator.validate(
        request("12345678901234", "123", "GBP", 1)));
    assertDoesNotThrow(() -> validator.validate(
        request("1234567890123456789", "123", "EUR", 1)));
  }

  @Test
  void rejectsCardNumberLongerThanNineteenDigits() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("12345678901234567890", "123", "GBP", 100)));
  }

  @Test
  void rejectsMissingRequiredFields() {
    assertThrows(InvalidPaymentRequestException.class, () -> validator.validate(new PostPaymentRequest()));
  }

  @Test
  void rejectsExpiryYearMonthThatHasPassed() {
    PostPaymentRequest request = request("1234567890123451", "123", "GBP", 100);
    request.setExpiryMonth(1);
    request.setExpiryYear(2000);
    assertThrows(InvalidPaymentRequestException.class, () -> validator.validate(request));
  }

  @Test
  void rejectsUnsupportedFourLetterCurrency() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123451", "123", "GBPX", 100)));
  }

  @Test
  void rejectsNegativeAmount() {
    assertThrows(InvalidPaymentRequestException.class,
        () -> validator.validate(request("1234567890123451", "123", "GBP", -1)));
  }

  private PostPaymentRequest request(String cardNumber, String cvv, String currency, int amount) {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(cardNumber);
    request.setExpiryMonth(12);
    request.setExpiryYear(2099);
    request.setCurrency(currency);
    request.setAmount(amount);
    request.setCvv(cvv);
    return request;
  }
}
