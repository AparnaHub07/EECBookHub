package com.eec.bookhub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="payments")
public class Payment {
    @Id public String id;
    @Column(nullable=false, unique=true) public String borrowingId;
    @Column(nullable=false) public String borrowerId;
    @Column(nullable=false) public String providerId;
    @Column(nullable=false) public Integer amountPaise;
    @Column(nullable=false) public String currency = "INR";
    @Column(nullable=false) public String status = "PENDING";
    public String providerUpiId;
    public String transactionReference;
    public Instant createdAt = Instant.now();
    public Instant reportedAt;
    public Instant confirmedAt;
}
