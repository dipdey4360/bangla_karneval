package com.bangla.karneval.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record MembershipSettingsRequest(
    @NotBlank @Size(max = 4000) String benefits,
    @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal singleFee,
    @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal coupleFee,
    @NotNull @Size(max = 2000) String paymentInstructions
) {}
