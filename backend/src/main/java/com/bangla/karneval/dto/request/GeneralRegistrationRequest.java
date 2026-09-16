package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class GeneralRegistrationRequest {

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

    private List<AdditionalParticipantRequest> additionalParticipants = new ArrayList<>();

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
