package com.eec.bookhub.service;

import com.eec.bookhub.model.*;
import com.eec.bookhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class BookService {
    private final BookRepository books;
    private final BorrowingRepository borrowings;
    private final UserRepository users;

    public BookService(BookRepository books, BorrowingRepository borrowings, UserRepository users) {
        this.books = books; this.borrowings = borrowings; this.users = users;
    }

    public List<Book> all(){ return books.findAllByOrderByAddedDateDesc(); }
    public Book get(String id){ return books.findById(id).orElseThrow(() -> new IllegalArgumentException("Book not found.")); }

    @Transactional
    public Book create(Book b, User u) {
        b.id = "b_" + UUID.randomUUID().toString().replace("-", "").substring(0,12);
        b.providerId = u.id;
        b.providerName = u.name;
        b.status = "Available";
        b.borrowCount = 0;
        b.addedDate = java.time.Instant.now();
        if (b.duration == null) b.duration = 14;
        if (b.pickupLocation == null || b.pickupLocation.isBlank()) throw new IllegalArgumentException("Pickup location is required.");
        if (b.returnLocation == null || b.returnLocation.isBlank()) throw new IllegalArgumentException("Return location is required.");
        return books.save(b);
    }

    @Transactional
    public Book updateStatus(String id, String status, User u) {
        Book b = get(id);
        if (!b.providerId.equals(u.id)) throw new SecurityException("Only the provider can change availability.");
        String normalized = status == null ? "" : status.trim();
        if (!normalized.equals("Available") && !normalized.equals("Not Available"))
            throw new IllegalArgumentException("Status must be Available or Not Available.");
        if (normalized.equals("Available") && borrowings.existsByBookIdAndStatusNot(id, "Returned"))
            throw new IllegalArgumentException("This book has an active borrowing and cannot be marked available yet.");
        b.status = normalized;
        return books.save(b);
    }

    @Transactional
    public void delete(String id, User u) {
        Book b = get(id);
        if (!b.providerId.equals(u.id)) throw new SecurityException("You can delete only your own listing.");
        if (borrowings.existsByBookIdAndStatusNot(id, "Returned")) throw new IllegalArgumentException("A borrowed book cannot be deleted.");
        books.delete(b);
    }

    public Map<String,Object> contact(String bookId,User requester){
        Book b=get(bookId);
        if(b.providerId.equals(requester.id)) return contactMap(b.providerId);
        boolean accepted=borrowings.findByBorrowerIdOrProviderIdOrderByBorrowDateDesc(requester.id,requester.id).stream()
                .anyMatch(x->x.bookId.equals(bookId)&&(x.borrowerId.equals(requester.id)||x.providerId.equals(requester.id)));
        if(!accepted) return Map.of("visible",false);
        return contactMap(b.providerId);
    }

    public Map<String,Object> borrowingContact(String borrowingId,User requester,String party){
        Borrowing bw=borrowings.findById(borrowingId).orElseThrow(()->new IllegalArgumentException("Borrowing not found."));
        if(!bw.borrowerId.equals(requester.id)&&!bw.providerId.equals(requester.id))throw new SecurityException("Not allowed.");
        String uid="borrower".equalsIgnoreCase(party)?bw.borrowerId:bw.providerId;
        return contactMap(uid);
    }

    private Map<String,Object> contactMap(String uid){
        User u=users.findById(uid).orElseThrow();
        return Map.of("visible",true,"name",u.name,"phone",u.phone);
    }
}
