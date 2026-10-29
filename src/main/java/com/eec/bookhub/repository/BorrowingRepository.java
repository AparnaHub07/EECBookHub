package com.eec.bookhub.repository;

import com.eec.bookhub.model.Borrowing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BorrowingRepository extends JpaRepository<Borrowing,String> {
    List<Borrowing> findByBorrowerIdOrProviderIdOrderByBorrowDateDesc(String borrowerId,String providerId);
    boolean existsByBookIdAndStatusNot(String bookId, String status);
}
