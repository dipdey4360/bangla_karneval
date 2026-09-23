package com.bangla.karneval.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record MembershipSettingsRequest(
    @NotBlank @Size(max = 4000) String benefits,
    @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal singleFee,
    @NotNull @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal coupleFee,
    @NotNull @Size(max = 2000) String paymentInstructions,
    @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal memberDiscountPercent
) {
    public MembershipSettingsRequest(String benefits, BigDecimal singleFee, BigDecimal coupleFee, String paymentInstructions) {
        this(benefits, singleFee, coupleFee, paymentInstructions, null);
    }
}
