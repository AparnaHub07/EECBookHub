package com.eec.bookhub.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "borrowings")
public class Borrowing {

    @Id
    public String id;

    @Column(nullable = false)
    public String bookId;

    @Column(nullable = false)
    public String borrowerId;

    @Column(nullable = false)
    public String providerId;

    @Column(nullable = false)
    public Instant borrowDate;

    @Column(nullable = false)
    public Instant dueDate;

    public Instant returnDate;

    @Column(nullable = false)
    public String status = "Active";

    @Column(nullable = false)
    public Integer lateDays = 0;

    @Column(nullable = false)
    public Integer penalty = 0;

    @Column(nullable = false)
    public String paymentStatus = "—";

    public String transactionId;
}