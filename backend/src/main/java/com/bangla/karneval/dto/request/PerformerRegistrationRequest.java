package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PerformerRegistrationRequest {

    @NotBlank
    private String name;

    @NotBlank @Email
    private String email;

    private String phone;

    @Past
    private LocalDate dateOfBirth;                 // ← changed from age

    private String address;

    @NotBlank
    private String performanceType;

    private String performanceDescription;
    private Integer groupMemberCount = 1;

    private List<GroupMemberRequest> groupMembers = new ArrayList<>();

    @Data
    public static class GroupMemberRequest {
        @NotBlank
        private String    name;
        private LocalDate dateOfBirth;
        private String    gender;
    }
}
