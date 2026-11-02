package com.example.allowance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AllowanceRequest(
        @NotBlank @Size(max = 100) String cardholderName,
        @NotNull @Pattern(regexp = "[0-9]{4}") String lastFour,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monthlyLimit,
        @NotNull @Pattern(regexp = "USD|EUR|GBP|CAD|AUD") String currency) {}
