package com.bangla.karneval.dto.request;

import com.bangla.karneval.model.PaymentStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// Omitted or null fields leave existing values unchanged; there is no default payment status.
public record RegistrationUpdateRequest(
        PaymentStatus paymentStatus,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        String address,
        String adminNote
) {}
