package com.bangla.karneval;

import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import com.bangla.karneval.scheduler.PaymentReminderScheduler;
import com.bangla.karneval.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Duration;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Opt-in: this test creates/drops tables in the isolated membership_test database only.
@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION", matches="true")
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:postgresql://127.0.0.1:15432/membership_test",
    "spring.datasource.username=postgres", "spring.datasource.password=membership_test_only",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.mail.username=test@example.invalid",
    "spring.mail.password=test-only", "jwt.secret=isolated-membership-test-key-at-least-64-characters-never-production",
    "logging.level.root=WARN", "logging.level.com.bangla.karneval=WARN"
})
@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class MembershipIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired EventConfigRepository configs;
    @MockBean JavaMailSender mail;
    @MockBean EmailService oldEmail;
    @MockBean PaymentReminderScheduler scheduler;

    @Test void applicationApprovalVisibilityRejectionAndEmailRetryPersist() throws Exception {
        EventConfig c=new EventConfig(); c.setEventYear(2026); configs.save(c);
        String application="""
            {"name":"Alice","partnerName":"Bob","dateOfBirth":"1990-01-01","address":"Test address",
            "partnerDateOfBirth":"1991-02-03","partnerAddress":"Partner street","partnerPhone":"987654","partnerEmail":"bob@example.invalid",
            "phone":"12345","email":"alice@example.invalid","membershipType":"COUPLE",
            "paymentMethod":"BANK_TRANSFER","paymentDeclared":true,"consent":true,"listed":true}
            """;
        String response=mvc.perform(post("/api/membership/applications").contentType("application/json").content(application))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.annualFee").value(30)).andReturn().getResponse().getContentAsString();
        long id=json.readTree(response).get("id").asLong();
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/admin/members/"+id+"/decision").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"status\":\"APPROVED\",\"paymentVerified\":false}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(mail);
        mvc.perform(put("/api/admin/members/"+id+"/decision").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"status\":\"APPROVED\",\"paymentVerified\":true,\"adminNote\":\"Welcome\"}"))
            .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).until(()->members.findById(id).orElseThrow().getEmailDelivery()==Member.Delivery.SENT);
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alice")).andExpect(jsonPath("$[1].name").value("Bob"))
            .andExpect(jsonPath("$[0].email").doesNotExist()).andExpect(jsonPath("$[0].dateOfBirth").doesNotExist());
        mvc.perform(put("/api/admin/members/"+id+"/visibility").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"listed\":false}" )).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(2));
        reset(mail); doThrow(new MailSendException("Simulated failure")).when(mail).send(any(SimpleMailMessage.class));
        mvc.perform(put("/api/admin/members/"+id+"/decision").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"status\":\"REJECTED\",\"paymentVerified\":true,\"adminNote\":\"Contact us\"}"))
            .andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).until(()->members.findById(id).orElseThrow().getEmailDelivery()==Member.Delivery.FAILED);
        reset(mail);
        mvc.perform(post("/api/admin/members/"+id+"/retry-email").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        await().atMost(Duration.ofSeconds(10)).until(()->members.findById(id).orElseThrow().getEmailDelivery()==Member.Delivery.SENT);
        assertEquals("Contact us",members.findById(id).orElseThrow().getAdminNote());
        verify(mail).send(argThat((SimpleMailMessage message)->message.getText().contains("rejected")));
        mvc.perform(put("/api/admin/membership/settings").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"benefits\":\"Voting\\nDiscounts\",\"singleFee\":27,\"coupleFee\":32,\"paymentInstructions\":\"Test instructions\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/membership/settings")).andExpect(jsonPath("$.singleFee").value(27));
        assertEquals(0,members.findById(id).orElseThrow().getAnnualFee().compareTo(new java.math.BigDecimal("30.00")));
    }
}
