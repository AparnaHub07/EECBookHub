package com.eec.bookhub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="users")
public class User {
    @Id public String id;
    @Column(nullable=false) public String name;
    @Column(nullable=false, unique=true) public String email;
    @Column(nullable=false) public String phone;
    @Column(nullable=false) public String dept;
    @Column(nullable=false) public String year;
    public String upiId = "";
    @JsonIgnore @Column(nullable=false) public String passwordHash;
    @Column(nullable=false) public Instant createdAt = Instant.now();
}
