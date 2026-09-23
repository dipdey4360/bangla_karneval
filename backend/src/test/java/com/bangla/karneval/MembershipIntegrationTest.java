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
    @org.springframework.test.context.DynamicPropertySource
    static void isolatedSchema(org.springframework.test.context.DynamicPropertyRegistry registry) {
        String url=IsolatedPostgres.newSchemaUrl();
        registry.add("spring.datasource.url", () -> url);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MemberRepository members;
    @Autowired EventConfigRepository configs;
    @Autowired RegistrationRepository registrations;
    @Autowired com.bangla.karneval.scheduler.MembershipExpiryScheduler expiryScheduler;
    @MockBean JavaMailSender mail;
    @MockBean EmailService oldEmail;
    @MockBean PaymentReminderScheduler scheduler;

    @Test void applicationApprovalVisibilityRejectionAndEmailRetryPersist() throws Exception {
        EventConfig c=new EventConfig(); c.setEventYear(2026); c.setPricePerPerson(new java.math.BigDecimal("20.00")); c.setRegistrationEnabled(true); configs.save(c);
        String application="""
            {"name":"Alice","partnerName":"Bob","dateOfBirth":"1990-01-01","address":"Test address",
            "partnerDateOfBirth":"1991-02-03","partnerPhone":"987654","partnerEmail":"bob@example.invalid",
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
        String membershipId = members.findById(id).orElseThrow().getMembershipId();
        assertEquals("BKM-00001", membershipId);
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        assertEquals(today,members.findById(id).orElseThrow().getMembershipStartsOn());
        assertEquals(today.plusYears(1),members.findById(id).orElseThrow().getMembershipExpiresOn());
        mvc.perform(get("/api/admin/members").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].membershipExpiresOn").value(today.plusYears(1).toString()))
            .andExpect(jsonPath("$[0].validityStatus").value("Active"));
        verify(mail).send(argThat((SimpleMailMessage message) -> message.getText().contains(membershipId)
            && message.getText().contains("confidential") && message.getText().contains(today.plusYears(1).toString()) && message.getTo().length == 2));
        assertNull(members.findById(id).orElseThrow().getPartnerAddress());
        mvc.perform(put("/api/admin/membership/settings").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("""
                {"benefits":"Discounts","singleFee":25,"coupleFee":30,"paymentInstructions":"Test","memberDiscountPercent":25}
                """)).andExpect(status().isOk());
        for (String name : new String[]{"Alice", "Bob"}) {
            mvc.perform(post("/api/register/verify-membership").contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("membershipId",membershipId,"name",name))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.discountPercent").value(25)).andExpect(jsonPath("$.email").doesNotExist());
        }
        String registration = """
            {"eventYear":2026,"primaryName":"Alice","primaryDateOfBirth":"1990-01-01","email":"alice@example.invalid",
            "membershipId":"%s","expectedTotal":30,"paymentMethod":"PAYPAL","consent":true,
            "additionalParticipants":[{"name":"Bob","dateOfBirth":"1991-02-03","membershipId":"%s"}]}
            """.formatted(membershipId,membershipId);
        mvc.perform(post("/api/register/general").contentType("application/json").content(registration))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(30));
        assertEquals(0,registrations.findAll().get(0).getMemberDiscountAmount().compareTo(new java.math.BigDecimal("10.00")));
        String partnerOnly = """
            {"eventYear":2026,"primaryName":"Bob","primaryDateOfBirth":"1991-02-03","email":"bob@example.invalid",
             "membershipId":"%s","expectedTotal":15,"paymentMethod":"PAYPAL","consent":true}
            """.formatted(membershipId);
        mvc.perform(post("/api/register/general").contentType("application/json").content(partnerOnly))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(15));

        mvc.perform(post("/api/register/general").contentType("application/json").content(registration.replace("\"expectedTotal\":30","\"expectedTotal\":1")))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/register/general").contentType("application/json").content(registration.replace("\"name\":\"Bob\"","\"name\":\"Alice\"")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/register/verify-membership").contentType("application/json")
            .content(json.writeValueAsString(java.util.Map.of("membershipId",membershipId,"name","Wrong name"))))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("Alice")).andExpect(jsonPath("$[1].name").value("Bob"))
            .andExpect(jsonPath("$[0].email").doesNotExist()).andExpect(jsonPath("$[0].dateOfBirth").doesNotExist());
        mvc.perform(put("/api/admin/members/"+id+"/visibility").with(user("admin").roles("ADMIN"))
            .contentType("application/json").content("{\"listed\":false}" )).andExpect(status().is4xxClientError());
        mvc.perform(get("/api/members")).andExpect(jsonPath("$.length()").value(2));
        // Simulate two daily runs at the reminder date and verify persisted delivery markers.
        reset(mail);
        var reminderDate = today.plusYears(1).minusMonths(1);
        expiryScheduler.sendDueReminders(reminderDate);
        expiryScheduler.sendDueReminders(reminderDate.plusDays(1));
        verify(mail,times(2)).send(any(SimpleMailMessage.class));
        var reminded = members.findById(id).orElseThrow();
        assertNotNull(reminded.getExpiryReminderSentAt()); assertNotNull(reminded.getPartnerExpiryReminderSentAt());
        assertTrue(members.findExpiryReminderCandidates(Member.Status.APPROVED, Member.Type.COUPLE,
            reminderDate,reminderDate.plusDays(32)).isEmpty());
        reminded.setMembershipStartsOn(today.minusYears(1)); reminded.setMembershipExpiresOn(today); members.save(reminded);
        mvc.perform(post("/api/register/verify-membership").contentType("application/json")
            .content(json.writeValueAsString(java.util.Map.of("membershipId",membershipId,"name","Alice"))))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/register/general").contentType("application/json").content(partnerOnly))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/members").with(user("admin").roles("ADMIN")))
            .andExpect(jsonPath("$[0].validityStatus").value("Expired"));
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
