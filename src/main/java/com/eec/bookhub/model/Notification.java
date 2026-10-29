package com.eec.bookhub.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="notifications")
public class Notification {
    @Id public String id;
    @Column(nullable=false) public String userId;
    @Column(nullable=false) public String type;
    @Column(nullable=false, length=1000) public String message;
    @Column(nullable=false) public Instant date = Instant.now();
    @Column(nullable=false) public boolean read = false;
}
