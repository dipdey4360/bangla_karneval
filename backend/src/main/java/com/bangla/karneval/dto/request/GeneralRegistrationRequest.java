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

    @NotBlank
    private String primaryName;

    @NotBlank @Email
    private String email;

    @NotNull @Past
    private LocalDate primaryDateOfBirth;

    private String gender;

    private String address;
    private String phone;

    @NotBlank
    private String paymentMethod;

    @NotNull
    private List<@NotNull @Valid AdditionalParticipantRequest> additionalParticipants = new ArrayList<>();

    @Data
    public static class AdditionalParticipantRequest {
        @NotBlank
        private String name;

        @NotNull @Past
        private LocalDate dateOfBirth;

        private String gender;
        private String relation;
    }
}
