package com.eec.bookhub.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos {
  private AuthDtos() {}
  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
  public record RegisterRequest(@NotBlank String name, @NotBlank @Email String email, @NotBlank @Pattern(regexp="^[0-9]{10}$") String phone,
                                @NotBlank String dept, @NotBlank String year, @Size(min=6) String password) {}
  public record ForgotRequest(@NotBlank @Email String email) {}
  public record ResetRequest(@NotBlank @Email String email, @NotBlank @Pattern(regexp="^[0-9]{6}$") String otp, @NotBlank @Size(min=6) String newPassword) {}
}
