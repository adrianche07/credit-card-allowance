package com.example.allowance;

import java.math.BigDecimal;

public record AllowanceResponse(Long id, String cardholderName, String lastFour,
                                BigDecimal monthlyLimit, String currency) {}
