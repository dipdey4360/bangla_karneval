package com.bangla.karneval.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventRequest {
    @NotNull  private Integer eventYear;
    private Long eventEditionId;
    private Long eventVersion;
    @NotBlank private String  category;
    @NotBlank private String  title;
    private String  description;
    private Boolean isHighlight = false;
}
