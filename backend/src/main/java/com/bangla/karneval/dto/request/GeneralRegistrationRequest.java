package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class GeneralRegistrationRequest {
    @NotNull private Integer eventYear;
    private Long eventEditionId;
    private Long eventVersion;

    @NotBlank
    private String primaryName;
    @Size(max = 40) private String membershipId;
    @DecimalMin("0") private java.math.BigDecimal expectedTotal;

    @NotBlank @Email
    private String email;

    @NotNull @Past
    private LocalDate primaryDateOfBirth;

    private String gender;

    private String address;
    private String phone;

    @NotBlank
    private String paymentMethod;

    @NotNull(message = "Data storage consent is required")
    @AssertTrue(message = "Data storage consent is required")
    private Boolean consent;

    @NotNull
    private List<@NotNull @Valid AdditionalParticipantRequest> additionalParticipants = new ArrayList<>();

    @Data
    public static class AdditionalParticipantRequest {
        @NotBlank
        private String name;
        @Size(max = 40) private String membershipId;

        @NotNull @Past
        private LocalDate dateOfBirth;

        private String gender;
        private String relation;
    }
}
