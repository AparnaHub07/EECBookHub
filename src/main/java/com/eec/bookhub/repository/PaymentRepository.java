package com.eec.bookhub.repository;

import com.eec.bookhub.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,String> {
    Optional<Payment> findByBorrowingId(String borrowingId);
}
