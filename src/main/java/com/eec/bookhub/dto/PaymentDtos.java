package com.eec.bookhub.dto;

import jakarta.validation.constraints.NotBlank;

public final class PaymentDtos {
    private PaymentDtos() {}
    public record ReportRequest(@NotBlank String borrowingId, @NotBlank String transactionReference) {}
    public record ConfirmRequest(@NotBlank String borrowingId, boolean accept) {}
}
