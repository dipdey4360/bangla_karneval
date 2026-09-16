package com.bangla.karneval.dto.request;
import com.bangla.karneval.model.Member;
import jakarta.validation.constraints.*;
public record MembershipDecisionRequest(
    @NotNull Member.Status status,
    boolean paymentVerified,
    @Size(max = 2000) String adminNote
) {}
