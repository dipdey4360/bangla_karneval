package com.bangla.karneval;
import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.scheduler.PaymentReminderScheduler;
import com.bangla.karneval.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION", matches="true")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:15432/membership_test",
    "spring.datasource.username=postgres", "spring.datasource.password=membership_test_only",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.mail.username=test@example.invalid",
    "spring.mail.password=test-only", "jwt.secret=isolated-membership-test-key-at-least-64-characters-never-production",
    "logging.level.root=WARN", "logging.level.com.bangla.karneval=WARN"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ActiveYearIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired EventConfigRepository configs;
    @Autowired RegistrationRepository registrations;
    @Autowired PerformerRegistrationRepository performers;
    @Autowired ApplicationSettingsRepository settings;
    @MockBean JavaMailSender mail;
    @MockBean EmailService oldEmail;
    @MockBean PaymentReminderScheduler scheduler;
    String registration(int year) { return """
        {"eventYear":%d,"primaryName":"Test","email":"test@example.invalid","primaryDateOfBirth":"1990-01-01","paymentMethod":"PAYPAL","additionalParticipants":[]}
        """.formatted(year); }
    String performer(int year) { return """
        {"eventYear":%d,"name":"Performer","email":"test@example.invalid","performanceType":"DANCE","groupMembers":[]}
        """.formatted(year); }
    @Test void switchYearPersistsAndLeavesHistoricalRecordsAndMembershipUntouched() throws Exception {
        for(int year:new int[]{2026,2027}) {var c=new EventConfig(); c.setEventYear(year); c.setPricePerPerson(new BigDecimal(year==2026?"10.00":"15.00")); configs.save(c);}
        mvc.perform(put("/api/admin/membership/settings").with(user("admin").roles("ADMIN")).contentType("application/json")
            .content("{\"benefits\":\"Voting\",\"singleFee\":25,\"coupleFee\":30,\"paymentInstructions\":\"Instructions\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/register/general").contentType("application/json").content(registration(2026))).andExpect(status().isOk());
        mvc.perform(post("/api/register/performer").contentType("application/json").content(performer(2026))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/config/active-year").with(user("visitor").roles("USER")).contentType("application/json").content("{\"eventYear\":2027}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/config/active-year").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"eventYear\":2027}")).andExpect(status().isOk());
        assertEquals(2027,settings.findById(1).orElseThrow().getActiveEventYear());
        mvc.perform(get("/api/config/current")).andExpect(jsonPath("$.eventYear").value(2027)).andExpect(jsonPath("$.pricePerPerson").value(15));
        mvc.perform(get("/api/membership/settings")).andExpect(jsonPath("$.singleFee").value(25)).andExpect(jsonPath("$.benefits").value("Voting"));
        mvc.perform(post("/api/register/general").contentType("application/json").content(registration(2026))).andExpect(status().isConflict());
        mvc.perform(post("/api/register/performer").contentType("application/json").content(performer(2026))).andExpect(status().isConflict());
        mvc.perform(post("/api/register/general").contentType("application/json").content(registration(2027))).andExpect(status().isOk());
        assertEquals(1,registrations.findByEventYear(2026).size());
        var newReg=registrations.findByEventYear(2027).get(0); assertTrue(newReg.getReferenceCode().contains("2027")); assertEquals(0,newReg.getCalculatedAmount().compareTo(new BigDecimal("15.00")));
        assertEquals(2026,performers.findAll().get(0).getEventYear());
        mvc.perform(get("/api/admin/registrations").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].eventYear").value(2027));
        mvc.perform(get("/api/admin/registrations?year=2026").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$[0].eventYear").value(2026));
        mvc.perform(get("/api/admin/dashboard/stats?year=2026").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.totalRegistrations").value(1));
        mvc.perform(get("/api/admin/performers").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/admin/performers?year=2026").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/admin/config/active-year").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"eventYear\":2099}")).andExpect(status().isBadRequest());
        assertEquals(2027,settings.findById(1).orElseThrow().getActiveEventYear());
    }
}
