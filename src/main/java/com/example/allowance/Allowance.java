package com.example.allowance;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "allowances")
class Allowance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String owner;
    @Column(nullable = false, length = 100)
    private String cardholderName;
    @Column(nullable = false, length = 4)
    private String lastFour;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyLimit;
    @Column(nullable = false, length = 3)
    private String currency;

    protected Allowance() {}

    Allowance(String owner, AllowanceRequest request) {
        this.owner = owner;
        update(request);
    }

    void update(AllowanceRequest request) {
        cardholderName = request.cardholderName().strip();
        lastFour = request.lastFour();
        monthlyLimit = request.monthlyLimit();
        currency = request.currency();
    }

    AllowanceResponse response() {
        return new AllowanceResponse(id, cardholderName, lastFour, monthlyLimit, currency);
    }
}
