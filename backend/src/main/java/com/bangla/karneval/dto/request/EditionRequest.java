package com.bangla.karneval.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record EditionRequest(
 @NotBlank @Size(max=60) String programmeCode,
 @NotNull @Min(2000) @Max(2200) Integer eventYear,
 @NotBlank @Size(max=255) String title,
 @Size(max=500) String tagline,
 @Size(max=10000) String aboutText,
 LocalDate eventDate, @Size(max=255) String eventLocation,
 @NotNull @DecimalMin("0") @DecimalMax("99999") @Digits(integer=5,fraction=2) BigDecimal pricePerPerson,
 boolean registrationEnabled, boolean performerEnabled,
 @NotBlank @Pattern(regexp="bangla-karneval|eid|puja|bangla-noboborsho|bbq|game") String themeKey,
 @NotBlank @Pattern(regexp="#[0-9a-fA-F]{6}") String accentColor,
 @Size(max=4000) String paymentInstructions,
 @Size(max=30) String contactPhone, @Email @Size(max=150) String contactEmail,
 @Size(max=30) String contactWhatsapp, @Size(max=255) String contactFacebook, @Size(max=255) String contactInstagram,
 Long version) {}
