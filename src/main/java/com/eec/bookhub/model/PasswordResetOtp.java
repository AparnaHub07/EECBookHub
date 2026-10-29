package com.eec.bookhub.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="password_reset_otps")
public class PasswordResetOtp {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String userId;
    @Column(nullable=false) public String otpHash;
    @Column(nullable=false) public Instant expiresAt;
    @Column(nullable=false) public int attempts = 0;
    @Column(nullable=false) public boolean used = false;
    @Column(nullable=false) public Instant createdAt = Instant.now();
}
