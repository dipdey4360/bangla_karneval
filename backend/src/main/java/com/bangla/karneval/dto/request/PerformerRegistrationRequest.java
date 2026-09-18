package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class PerformerRegistrationRequest {
    @NotNull private Integer eventYear;
    private Long eventEditionId;
    private Long eventVersion;

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
    @Min(1)
    private Integer groupMemberCount;

    @NotNull
    private List<@NotNull @Valid GroupMemberRequest> groupMembers = new ArrayList<>();

    @AssertTrue(message = "Group member count must include the main performer and all listed group members")
    public boolean isGroupCountConsistent() {
        return groupMembers == null || groupMemberCount == null || groupMemberCount == groupMembers.size() + 1;
    }

    @Data
    public static class GroupMemberRequest {
        @NotBlank
        private String    name;
        @Past
        private LocalDate dateOfBirth;
        private String    gender;
    }
}
