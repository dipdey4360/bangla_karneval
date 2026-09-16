package com.bangla.karneval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long       totalRegistrations;
    private Long       totalPaid;
    private Long       totalUnpaid;
    private Long       totalOverdue;
    private BigDecimal totalRevenue;
    private int       childrenCount;
    private int       adultsCount;
    private int       seniorsCount;
}
