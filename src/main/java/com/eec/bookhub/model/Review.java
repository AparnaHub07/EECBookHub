package com.eec.bookhub.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="reviews")
public class Review {
    @Id public String id;
    @Column(nullable=false) public String bookId;
    @Column(nullable=false) public String userId;
    @Column(nullable=false) public String userName;
    @Column(nullable=false) public Integer rating;
    @Column(length=1000) public String comment;
    @Column(nullable=false) public Instant date = Instant.now();
}
