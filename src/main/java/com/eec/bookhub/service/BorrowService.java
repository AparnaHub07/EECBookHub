
package com.eec.bookhub.service;

import com.eec.bookhub.model.*;
import com.eec.bookhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BorrowService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int BASE_PENALTY = 40;
    private static final int PENALTY_STEP = 10;

    private final BorrowRequestRepository requests;
    private final BorrowingRepository borrowings;
    private final BookRepository books;
    private final NotificationService notifications;

    public BorrowService(
            BorrowRequestRepository requests,
            BorrowingRepository borrowings,
            BookRepository books,
            NotificationService notifications) {
        this.requests = requests;
        this.borrowings = borrowings;
        this.books = books;
        this.notifications = notifications;
    }

    // REQUESTS
    public List<BorrowRequest> requestsFor(User u) {
        return requests.findByBorrowerIdOrProviderIdOrderByRequestDateDesc(
                u.id, u.id);
    }

    // BORROWINGS
    @Transactional
    public List<Borrowing> borrowingsFor(User u) {
        List<Borrowing> list =
                borrowings.findByBorrowerIdOrProviderIdOrderByBorrowDateDesc(
                        u.id, u.id);

        Instant now = Instant.now();

        for (Borrowing bw : list) {
            if (!"Returned".equals(bw.status)) {
                refreshPenalty(bw, now);
                borrowings.save(bw);
            }
        }
        return list;
    }

    // CREATE BORROW REQUEST
    @Transactional
    public BorrowRequest request(String bookId, User u) {
        Book b = books.findById(bookId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Book not found."));

        if (b.providerId.equals(u.id)) {
            throw new IllegalArgumentException(
                    "You cannot borrow your own book.");
        }

        if (!"Available".equals(b.status)) {
            throw new IllegalArgumentException(
                    "This book is not available.");
        }

        if (requests.existsByBookIdAndBorrowerIdAndStatus(
                bookId, u.id, "Pending")) {
            throw new IllegalArgumentException(
                    "You already have a pending request for this book.");
        }

        BorrowRequest r = new BorrowRequest();
        r.id = "rq_" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12);
        r.bookId = b.id;
        r.borrowerId = u.id;
        r.borrowerName = u.name;
        r.providerId = b.providerId;
        r.duration = b.duration;

        requests.save(r);

        notifications.send(
                b.providerId,
                "request",
                u.name + " requested to borrow \"" + b.title + "\".");

        return r;
    }

    // ACCEPT / REJECT BORROW REQUEST
    @Transactional
    public BorrowRequest respond(String id, boolean accept, User u) {
        BorrowRequest r = requests.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Request not found."));

        if (!r.providerId.equals(u.id)) {
            throw new SecurityException("Only the provider can respond.");
        }

        if (!"Pending".equals(r.status)) {
            throw new IllegalArgumentException(
                    "This request has already been handled.");
        }

        Book b = books.findById(r.bookId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Book not found."));

        if (accept) {
            if (!"Available".equals(b.status)) {
                throw new IllegalArgumentException(
                        "The book is no longer available.");
            }

            r.status = "Accepted";
            b.status = "Not Available";

            if (b.borrowCount == null) {
                b.borrowCount = 0;
            }
            b.borrowCount = b.borrowCount + 1;

            Borrowing bw = new Borrowing();
            bw.id = "bw_" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12);
            bw.bookId = b.id;
            bw.borrowerId = r.borrowerId;
            bw.providerId = r.providerId;

            Instant now = Instant.now();
            bw.borrowDate = now;
            bw.dueDate = now.plus(r.duration, ChronoUnit.DAYS);
            bw.returnDate = null;
            bw.status = "Active";
            bw.lateDays = 0;
            bw.penalty = 0;
            bw.paymentStatus = "—";
            bw.transactionId = null;

            borrowings.save(bw);
            books.save(b);

            notifications.send(
                    r.borrowerId,
                    "accepted",
                    b.providerName + " accepted your request for \"" +
                            b.title + "\". Contact details are now visible.");
        } else {
            r.status = "Rejected";
            notifications.send(
                    r.borrowerId,
                    "rejected",
                    "Your request for \"" + b.title + "\" was declined.");
        }

        return requests.save(r);
    }

    // PENALTY CALCULATION
    public void refreshPenalty(Borrowing bw, Instant asOf) {
        if (bw.dueDate == null) {
            return;
        }

        LocalDate dueDate = bw.dueDate.atZone(APP_ZONE).toLocalDate();
        LocalDate currentDate = asOf.atZone(APP_ZONE).toLocalDate();

        long late = ChronoUnit.DAYS.between(dueDate, currentDate);
        late = Math.max(0, late);
        bw.lateDays = (int) late;

        if (late > 0) {
            bw.penalty = BASE_PENALTY
                    + (int) ((late - 1) * PENALTY_STEP);
        } else {
            bw.penalty = 0;
        }

        if (!"Paid".equals(bw.paymentStatus)
                && !"Verification Pending".equals(bw.paymentStatus)) {
            bw.paymentStatus = bw.penalty > 0 ? "Pending" : "—";
        }
    }

    // BORROWER REQUESTS RETURN
    // The book remains unavailable until the provider confirms.
    @Transactional
    public Borrowing requestReturn(String id, User u) {
        Borrowing bw = borrowings.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Borrowing not found."));

        if (!bw.borrowerId.equals(u.id)) {
            throw new SecurityException(
                    "Only the borrower can request a return.");
        }

        if ("Returned".equals(bw.status)
                || "Return Requested".equals(bw.status)) {
            return bw;
        }

        if (!"Active".equals(bw.status)) {
            throw new IllegalArgumentException(
                    "Only active borrowings can request a return.");
        }

        Book b = books.findById(bw.bookId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Book not found."));

        bw.status = "Return Requested";
        borrowings.save(bw);

        notifications.send(
                bw.providerId,
                "returned",
                u.name + " has requested to return \"" + b.title +
                        "\". Confirm after physically receiving the book.");

        return bw;
    }

    // PROVIDER CONFIRMS RETURN
    // The book becomes available only after provider confirmation.
    @Transactional
    public Borrowing confirmReturn(String id, User u) {
        Borrowing bw = borrowings.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Borrowing not found."));

        if (!bw.providerId.equals(u.id)) {
            throw new SecurityException(
                    "Only the provider can confirm the return.");
        }

        if ("Returned".equals(bw.status)) {
            return bw;
        }

        if (!"Return Requested".equals(bw.status)) {
            throw new IllegalArgumentException(
                    "The borrower has not requested a return.");
        }

        Book b = books.findById(bw.bookId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Book not found."));

        Instant now = Instant.now();
        refreshPenalty(bw, now);

        bw.returnDate = now;
        bw.status = "Returned";
        b.status = "Available";

        books.save(b);
        borrowings.save(bw);

        notifications.send(
                bw.borrowerId,
                "returned",
                "The provider confirmed the return of \"" + b.title + "\"."
                        + (bw.penalty != null && bw.penalty > 0
                        ? " Pending penalty: ₹" + bw.penalty + "."
                        : " No penalty is due."));

        return bw;
    }

    // EXISTING RETURN METHOD — KEPT FOR COMPATIBILITY
    @Transactional
    public Borrowing markReturned(String id, User u) {
        Borrowing bw = borrowings.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Borrowing not found."));

        if (!bw.borrowerId.equals(u.id)) {
            throw new SecurityException(
                    "Only the borrower can confirm return.");
        }

        if ("Returned".equals(bw.status)) {
            return bw;
        }

        Instant now = Instant.now();
        refreshPenalty(bw, now);
        bw.returnDate = now;
        bw.status = "Returned";

        Book b = books.findById(bw.bookId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Book not found."));

        b.status = "Available";
        books.save(b);
        borrowings.save(bw);

        notifications.send(
                bw.providerId,
                "returned",
                "Your book \"" + b.title + "\" was returned"
                        + (bw.penalty != null && bw.penalty > 0
                        ? " with a pending penalty of ₹" + bw.penalty
                        : " on time") + ".");

        return bw;
    }
}
