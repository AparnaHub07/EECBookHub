
package com.eec.bookhub.controller;

import com.eec.bookhub.model.*;
import com.eec.bookhub.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class BorrowController {

    private final BorrowService service;
    private final CurrentUserService current;

    public BorrowController(
            BorrowService service,
            CurrentUserService current) {

        this.service = service;
        this.current = current;
    }

    // =========================================================
    // BORROW REQUESTS
    // =========================================================

    @GetMapping("/borrow-requests")
    public List<BorrowRequest> requests(
            HttpSession session) {

        return service.requestsFor(
                current.require(session)
        );
    }

    @PostMapping("/borrow-requests")
    public BorrowRequest request(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        return service.request(
                body.get("bookId"),
                current.require(session)
        );
    }

    @PostMapping("/borrow-requests/{id}/respond")
    public BorrowRequest respond(
            @PathVariable String id,
            @RequestBody Map<String, Boolean> body,
            HttpSession session) {

        return service.respond(
                id,
                Boolean.TRUE.equals(body.get("accept")),
                current.require(session)
        );
    }

    // =========================================================
    // BORROWINGS
    // =========================================================

    @GetMapping("/borrowings")
    public List<Borrowing> borrowings(
            HttpSession session) {

        // Refreshes penalties through the existing service logic.
        return service.borrowingsFor(
                current.require(session)
        );
    }

    // =========================================================
    // EXISTING RETURN ENDPOINT
    // =========================================================

    @PostMapping("/borrowings/{id}/return")
    public Borrowing returned(
            @PathVariable String id,
            HttpSession session) {

        return service.markReturned(
                id,
                current.require(session)
        );
    }

    // =========================================================
    // REQUEST RETURN — BORROWER
    // =========================================================

    @PostMapping("/borrowings/{id}/request-return")
    public Borrowing requestReturn(
            @PathVariable String id,
            HttpSession session) {

        return service.requestReturn(
                id,
                current.require(session)
        );
    }

    // =========================================================
    // CONFIRM RETURN — PROVIDER
    // =========================================================

    @PostMapping("/borrowings/{id}/confirm-return")
    public Borrowing confirmReturn(
            @PathVariable String id,
            HttpSession session) {

        return service.confirmReturn(
                id,
                current.require(session)
        );
    }
}
