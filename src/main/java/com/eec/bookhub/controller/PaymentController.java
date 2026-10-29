package com.eec.bookhub.controller;

import com.eec.bookhub.dto.PaymentDtos.*;
import com.eec.bookhub.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/payments/upi")
public class PaymentController {
    private final PaymentService payments;
    private final CurrentUserService current;

    public PaymentController(PaymentService payments, CurrentUserService current) {
        this.payments = payments;
        this.current = current;
    }

    @GetMapping("/{borrowingId}")
    public Map<String,Object> details(@PathVariable String borrowingId, HttpSession s) {
        return payments.details(borrowingId, current.require(s));
    }

    @PostMapping("/report")
    public Map<String,Object> report(@Valid @RequestBody ReportRequest r, HttpSession s) {
        return payments.report(r.borrowingId(), r.transactionReference(), current.require(s));
    }

    @PostMapping("/confirm")
    public Map<String,Object> confirm(@Valid @RequestBody ConfirmRequest r, HttpSession s) {
        return payments.confirm(r.borrowingId(), r.accept(), current.require(s));
    }
}
