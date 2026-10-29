package com.eec.bookhub.service;

import com.eec.bookhub.model.*;
import com.eec.bookhub.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PaymentService {

    private static final ZoneId APP_ZONE =
            ZoneId.of("Asia/Kolkata");

    private static final int BASE_PENALTY = 40;
    private static final int PENALTY_STEP = 10;

    private final PaymentRepository payments;
    private final BorrowingRepository borrowings;
    private final UserRepository users;
    private final NotificationService notifications;

    public PaymentService(
            PaymentRepository payments,
            BorrowingRepository borrowings,
            UserRepository users,
            NotificationService notifications) {

        this.payments = payments;
        this.borrowings = borrowings;
        this.users = users;
        this.notifications = notifications;
    }

    // =========================================================
    // FIND BORROWING
    // =========================================================

    private Borrowing borrowing(String id) {

        return borrowings.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Borrowing not found."
                        ));
    }

    // =========================================================
    // REFRESH CURRENT PENALTY
    // =========================================================

    private void refreshCurrentPenalty(Borrowing bw) {

        if (bw.dueDate == null) {
            return;
        }

        /*
         * If the penalty is already paid, don't change it.
         */
        if ("Paid".equals(bw.paymentStatus)) {
            return;
        }

        LocalDate dueDate =
                bw.dueDate
                        .atZone(APP_ZONE)
                        .toLocalDate();

        LocalDate currentDate =
                Instant.now()
                        .atZone(APP_ZONE)
                        .toLocalDate();

        long lateDays =
                ChronoUnit.DAYS.between(
                        dueDate,
                        currentDate
                );

        lateDays = Math.max(0, lateDays);

        bw.lateDays = (int) lateDays;

        if (lateDays > 0) {

            bw.penalty =
                    BASE_PENALTY +
                            (int) ((lateDays - 1)
                                    * PENALTY_STEP);

        } else {

            bw.penalty = 0;
        }

        if (!"Verification Pending".equals(
                bw.paymentStatus)) {

            bw.paymentStatus =
                    bw.penalty > 0
                            ? "Pending"
                            : "—";
        }

        borrowings.save(bw);
    }

    // =========================================================
    // CREATE / GET PAYMENT
    // =========================================================

    private Payment ensurePayment(Borrowing bw) {

        /*
         * Recalculate the penalty before checking it.
         *
         * This fixes the problem where the database still had
         * penalty = 0 even though the due date had passed.
         */
        refreshCurrentPenalty(bw);

        if (bw.penalty == null || bw.penalty <= 0) {

            throw new IllegalArgumentException(
                    "There is no penalty for this borrowing."
            );
        }

        if ("—".equals(bw.paymentStatus)) {

            bw.paymentStatus = "Pending";

            borrowings.save(bw);
        }

        // =====================================================
        // GET CURRENT PROVIDER
        // =====================================================

        User provider =
                users.findById(bw.providerId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Provider not found."
                                ));

        /*
         * IMPORTANT:
         *
         * Always get the CURRENT UPI ID from the user's profile.
         * This means if the provider changes their UPI ID later,
         * BookHub will use the new UPI ID.
         */
        String currentUpi =
                provider.upiId == null
                        ? ""
                        : provider.upiId.trim();

        if (currentUpi.isBlank()) {

            throw new IllegalArgumentException(
                    "The provider has not added a UPI ID yet. Please contact the provider."
            );
        }

        // =====================================================
        // FIND EXISTING PAYMENT
        // =====================================================

        Payment p =
                payments
                        .findByBorrowingId(bw.id)
                        .orElse(null);

        // =====================================================
        // CREATE NEW PAYMENT
        // =====================================================

        if (p == null) {

            p = new Payment();

            p.id =
                    "pay_" +
                            UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .substring(0, 12);

            p.borrowingId = bw.id;

            p.borrowerId = bw.borrowerId;

            p.providerId = bw.providerId;

            p.amountPaise =
                    bw.penalty * 100;

            p.providerUpiId = currentUpi;

            p.status = "PENDING";

            return payments.save(p);
        }

        // =====================================================
        // IMPORTANT FIX:
        // UPDATE OLD PAYMENT WITH CURRENT UPI ID
        // =====================================================

        if (!Objects.equals(
                p.providerUpiId,
                currentUpi)) {

            p.providerUpiId = currentUpi;

            payments.save(p);
        }

        // =====================================================
        // KEEP PAYMENT AMOUNT UPDATED
        // =====================================================

        int currentAmountPaise =
                bw.penalty * 100;

        if (!Objects.equals(
                p.amountPaise,
                currentAmountPaise)) {

            p.amountPaise =
                    currentAmountPaise;

            payments.save(p);
        }

        return p;
    }

    // =========================================================
    // PAYMENT DETAILS
    // =========================================================

    public Map<String, Object> details(
            String borrowingId,
            User user) {

        Borrowing bw =
                borrowing(borrowingId);

        if (!bw.borrowerId.equals(user.id)
                && !bw.providerId.equals(user.id)) {

            throw new SecurityException(
                    "Not allowed."
            );
        }

        Payment p =
                ensurePayment(bw);

        /*
         * Keep payment amount synchronized with
         * the latest penalty.
         */
        int currentAmountPaise =
                bw.penalty * 100;

        if (!Objects.equals(
                p.amountPaise,
                currentAmountPaise)) {

            p.amountPaise =
                    currentAmountPaise;

            payments.save(p);
        }

        // =====================================================
        // GET CURRENT PROVIDER AGAIN
        // =====================================================

        User provider =
                users.findById(bw.providerId)
                        .orElseThrow();

        /*
         * Always make sure the response contains the
         * latest UPI ID from the provider profile.
         */
        String currentUpi =
                provider.upiId == null
                        ? ""
                        : provider.upiId.trim();

        if (!Objects.equals(
                p.providerUpiId,
                currentUpi)) {

            p.providerUpiId = currentUpi;

            payments.save(p);
        }

        Map<String, Object> out =
                new LinkedHashMap<>();

        out.put(
                "ok",
                true
        );

        out.put(
                "borrowingId",
                bw.id
        );

        out.put(
                "amount",
                bw.penalty
        );

        out.put(
                "currency",
                p.currency
        );

        out.put(
                "providerName",
                provider.name
        );

        out.put(
                "providerUpiId",
                currentUpi
        );

        out.put(
                "status",
                p.status
        );

        out.put(
                "transactionReference",
                p.transactionReference == null
                        ? ""
                        : p.transactionReference
        );

        return out;
    }

    // =========================================================
    // REPORT UPI PAYMENT
    // =========================================================

    @Transactional
    public Map<String, Object> report(
            String borrowingId,
            String reference,
            User user) {

        Borrowing bw =
                borrowing(borrowingId);

        if (!bw.borrowerId.equals(user.id)) {

            throw new SecurityException(
                    "Only the borrower can submit payment details."
            );
        }

        /*
         * Calculate latest penalty before payment.
         */
        refreshCurrentPenalty(bw);

        if ("Paid".equals(bw.paymentStatus)) {

            throw new IllegalArgumentException(
                    "This penalty is already paid."
            );
        }

        if (bw.penalty == null
                || bw.penalty <= 0) {

            throw new IllegalArgumentException(
                    "There is no pending penalty for this borrowing."
            );
        }

        String ref =
                reference == null
                        ? ""
                        : reference.trim();

        if (!ref.matches(
                "^[A-Za-z0-9-]{6,40}$")) {

            throw new IllegalArgumentException(
                    "Enter a valid UPI transaction/reference number."
            );
        }

        Payment p =
                ensurePayment(bw);

        p.transactionReference = ref;

        p.status = "REPORTED";

        p.reportedAt = Instant.now();

        payments.save(p);

        bw.transactionId = ref;

        bw.paymentStatus =
                "Verification Pending";

        borrowings.save(bw);

        notifications.send(
                bw.providerId,
                "payment",
                "A borrower reported a UPI penalty payment of ₹"
                        + bw.penalty
                        + ". Reference: "
                        + ref
                        + ". Please verify it in your UPI app and confirm in EEC BookHub."
        );

        return Map.of(
                "ok",
                true,
                "status",
                "Verification Pending",
                "transactionReference",
                ref
        );
    }

    // =========================================================
    // CONFIRM / REJECT UPI PAYMENT
    // =========================================================

    @Transactional
    public Map<String, Object> confirm(
            String borrowingId,
            boolean accept,
            User user) {

        Borrowing bw =
                borrowing(borrowingId);

        if (!bw.providerId.equals(user.id)) {

            throw new SecurityException(
                    "Only the book provider can confirm this payment."
            );
        }

        Payment p =
                payments.findByBorrowingId(
                        borrowingId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "No reported payment found."
                        ));

        if (!"REPORTED".equals(p.status)) {

            throw new IllegalArgumentException(
                    "There is no payment waiting for verification."
            );
        }

        // =====================================================
        // ACCEPT PAYMENT
        // =====================================================

        if (accept) {

            p.status = "PAID";

            p.confirmedAt = Instant.now();

            bw.paymentStatus = "Paid";

            payments.save(p);

            borrowings.save(bw);

            notifications.send(
                    bw.borrowerId,
                    "payment",
                    "Your UPI penalty payment of ₹"
                            + bw.penalty
                            + " was confirmed by the book provider."
            );

            return Map.of(
                    "ok",
                    true,
                    "status",
                    "Paid"
            );
        }

        // =====================================================
        // REJECT PAYMENT
        // =====================================================

        p.status = "REJECTED";

        bw.paymentStatus = "Pending";

        payments.save(p);

        borrowings.save(bw);

        notifications.send(
                bw.borrowerId,
                "payment",
                "The provider could not verify your reported UPI payment. Please check the transaction reference and try again."
        );

        return Map.of(
                "ok",
                true,
                "status",
                "Pending"
        );
    }
}