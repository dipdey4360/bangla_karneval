package com.bangla.karneval.dto.request;
import com.bangla.karneval.model.Member;
import com.bangla.karneval.model.PaymentMethod;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record MembershipRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 100) String partnerName,
    @Past LocalDate partnerDateOfBirth,
    @Size(max = 500) String partnerAddress,
    @Size(max = 30) String partnerPhone,
    @Email @Size(max = 150) String partnerEmail,
    @NotNull @Past LocalDate dateOfBirth,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Size(max = 30) String phone,
    @NotBlank @Email @Size(max = 150) String email,
    @NotNull Member.Type membershipType,
    @NotNull PaymentMethod paymentMethod,
    @NotNull @AssertTrue(message = "Please confirm you have made the membership donation") Boolean paymentDeclared,
    @NotNull @AssertTrue(message = "Consent is required to submit an application") Boolean consent,
    @NotNull(message = "Please choose whether to display your name publicly") Boolean listed
) {}
