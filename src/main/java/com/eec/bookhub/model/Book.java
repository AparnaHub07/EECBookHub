package com.eec.bookhub.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="books")
public class Book {
    @Id public String id;
    @Column(nullable=false) public String title;
    @Column(nullable=false) public String author;
    public String isbn;
    @Column(nullable=false) public String category;
    @Column(length=1500) public String description;
    @Column(name="book_condition", nullable=false) public String condition;
    public String edition;
    @Column(nullable=false) public Integer duration = 14;
    @Column(nullable=false) public String pickupLocation;
    @Column(nullable=false) public String returnLocation;
    @Column(nullable=false) public String status = "Available";
    @Column(nullable=false) public String providerId;
    @Column(nullable=false) public String providerName;
    @Column(nullable=false) public Instant addedDate = Instant.now();
    @Column(nullable=false) public Integer borrowCount = 0;
}
