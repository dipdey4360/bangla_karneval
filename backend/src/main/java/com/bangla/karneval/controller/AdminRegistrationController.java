package com.bangla.karneval.controller;

import com.bangla.karneval.dto.response.ApiResponse;
import com.bangla.karneval.dto.request.RegistrationUpdateRequest;
import jakarta.validation.Valid;
import com.bangla.karneval.dto.response.DashboardStatsResponse;
import com.bangla.karneval.model.PaymentStatus;
import com.bangla.karneval.model.Registration;
import com.bangla.karneval.repository.RegistrationRepository;
import com.bangla.karneval.service.PriceCalculationService;
import com.bangla.karneval.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Period;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRegistrationController {

    @Autowired private RegistrationService     registrationService;
    @Autowired private RegistrationRepository  registrationRepository;
    @Autowired private PriceCalculationService priceCalculationService;
    @Autowired private com.bangla.karneval.service.ApplicationSettingsService settings;
    @Autowired private com.bangla.karneval.service.ProgrammeService programmes;

    @GetMapping("/registrations")
    public ResponseEntity<List<Registration>> getRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer year, @RequestParam(required = false) Long eventEditionId) {
        PaymentStatus ps = (status != null && !status.equalsIgnoreCase("ALL"))
                ? PaymentStatus.valueOf(status) : null;
        return ResponseEntity.ok(registrationService.searchByEdition(search, ps, programmes.resolve(eventEditionId,year)));
    }

    @PutMapping("/registrations/{id}")
    public ResponseEntity<Registration> updateRegistration(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationUpdateRequest request) {
        return ResponseEntity.ok(registrationService.updateRegistration(id, request));
    }

    @PutMapping("/registrations/{id}/payment")
    public ResponseEntity<ApiResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        registrationService.updatePaymentStatus(id, PaymentStatus.valueOf(status));
        return ResponseEntity.ok(ApiResponse.ok("Donation status updated"));
    }

    @DeleteMapping("/registrations/{id}")
    public ResponseEntity<ApiResponse> deleteRegistration(@PathVariable Long id) {
        registrationService.deleteRegistration(id);
        return ResponseEntity.ok(ApiResponse.ok("Registration deleted"));
    }

    @GetMapping("/registrations/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Integer year, @RequestParam(required = false) Long eventEditionId) {
        List<Registration> all = registrationRepository.findByEventEditionId(programmes.resolve(eventEditionId,year));
        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(sw);

        pw.println("Reference Code,Name,Email,Phone,Date of Birth,Age,Participants," +
                "Amount,Donation Method,Donation Status,Address,Registered At");

        for (Registration r : all) {
            int age = 0;
            if (r.getPrimaryDateOfBirth() != null) {
                age = Period.between(r.getPrimaryDateOfBirth(), LocalDate.now()).getYears();
            }
            pw.printf("%s,%s,%s,%s,%s,%d,%d,%.2f,%s,%s,%s,%s%n",
                    r.getReferenceCode(),
                    r.getPrimaryName(),
                    r.getEmail(),
                    r.getPhone()             != null ? r.getPhone()                          : "",
                    r.getPrimaryDateOfBirth() != null ? r.getPrimaryDateOfBirth().toString() : "",
                    age,
                    r.getParticipantCount(),
                    r.getCalculatedAmount(),
                    r.getPaymentMethod()     != null ? r.getPaymentMethod()                  : "",
                    r.getPaymentStatus(),
                    r.getAddress()           != null ? r.getAddress()                        : "",
                    r.getRegisteredAt()
            );
        }

        byte[] csvBytes = sw.toString().getBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "registrations.csv");
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats(@RequestParam(required = false) Integer year, @RequestParam(required = false) Long eventEditionId) {
        Long selectedEdition = programmes.resolve(eventEditionId,year);
        List<Registration> all = registrationRepository.findByEventEditionId(selectedEdition);
        PriceCalculationService.AgeGroupStats ageStats =
                priceCalculationService.getAgeGroupStats(all);

        BigDecimal revenue = all.stream().filter(r->r.getPaymentStatus()==PaymentStatus.CONFIRMED).map(Registration::getCalculatedAmount).reduce(BigDecimal.ZERO,BigDecimal::add);

        DashboardStatsResponse stats = new DashboardStatsResponse(
                (long) all.size(),
                all.stream().filter(r -> r.getPaymentStatus() == PaymentStatus.CONFIRMED).count(),
                all.stream().filter(r -> r.getPaymentStatus() == PaymentStatus.PENDING).count(),
                all.stream().filter(r -> r.getPaymentStatus() == PaymentStatus.OVERDUE).count(),
                revenue != null ? revenue : BigDecimal.ZERO,
                ageStats.getChildren(),
                ageStats.getAdults(),
                ageStats.getSeniors()
        );
        return ResponseEntity.ok(stats);
    }
}
