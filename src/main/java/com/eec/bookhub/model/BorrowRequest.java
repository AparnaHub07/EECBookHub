package com.eec.bookhub.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="borrow_requests")
public class BorrowRequest {
    @Id public String id;
    @Column(nullable=false) public String bookId;
    @Column(nullable=false) public String borrowerId;
    @Column(nullable=false) public String borrowerName;
    @Column(nullable=false) public String providerId;
    @Column(nullable=false) public String status = "Pending";
    @Column(nullable=false) public Instant requestDate = Instant.now();
    @Column(nullable=false) public Integer duration = 14;
}
