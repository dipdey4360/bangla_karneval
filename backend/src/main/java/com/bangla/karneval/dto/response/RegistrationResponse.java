package com.bangla.karneval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RegistrationResponse {
    private Long       id;
    private BigDecimal totalAmount;
    private String     referenceCode;
    private String     message;
}
