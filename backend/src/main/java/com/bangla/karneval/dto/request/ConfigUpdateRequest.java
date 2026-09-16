package com.bangla.karneval.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ConfigUpdateRequest {
    private BigDecimal pricePerPerson;
    private LocalDate  eventDate;
    private String     eventLocation;
    private String     aboutText;
    private String     contactPhone;
    private String     contactEmail;
    private String     contactWhatsapp;
    private String     contactFacebook;
    private String     contactInstagram;
}
