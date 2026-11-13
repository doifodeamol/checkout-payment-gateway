package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Locale;

/** Incoming merchant request. Sensitive card data is never logged or persisted. */
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  private String cardNumber;

  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  private Integer expiryYear;

  private String currency;
  private Integer amount;
  private String cvv;

  public String getCardNumber() { return cardNumber; }
  public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
  public Integer getExpiryMonth() { return expiryMonth; }
  public void setExpiryMonth(Integer expiryMonth) { this.expiryMonth = expiryMonth; }
  public Integer getExpiryYear() { return expiryYear; }
  public void setExpiryYear(Integer expiryYear) { this.expiryYear = expiryYear; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public Integer getAmount() { return amount; }
  public void setAmount(Integer amount) { this.amount = amount; }
  public String getCvv() { return cvv; }
  public void setCvv(String cvv) { this.cvv = cvv; }

  /** Formats the merchant month/year fields for the acquiring-bank contract. */
  public String getExpiryDate() {
    if (expiryMonth == null || expiryYear == null) {
      return null;
    }
    return String.format(Locale.ROOT, "%02d/%04d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber=[REDACTED], expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear + ", currency='" + currency + '\'' +
        ", amount=" + amount + ", cvv=[REDACTED]}";
  }
}
