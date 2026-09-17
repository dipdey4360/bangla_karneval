package com.bangla.karneval.controller;
import com.bangla.karneval.config.SecurityConfig;
import com.bangla.karneval.model.Member;
import com.bangla.karneval.service.*;
import com.bangla.karneval.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MembershipController.class)
@Import(SecurityConfig.class)
class MembershipControllerTest {
    @Autowired MockMvc mvc;
    @MockBean MembershipService service;
    @MockBean BoardMemberService board;
    @MockBean JwtUtil jwt;
    @Test void onlyAdminCanDeleteMembership() throws Exception {
        mvc.perform(delete("/api/admin/members/7")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/members/7").with(user("visitor").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        mvc.perform(delete("/api/admin/members/7").with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
        verify(service).delete(7L);
    }
    @Test void deletingMissingMembershipReturnsNotFound() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Membership application not found"))
            .when(service).delete(7L);
        mvc.perform(delete("/api/admin/members/7").with(user("admin").roles("ADMIN")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Membership application not found"));
    }
    private String application(boolean consent) { return """
        {"name":"Alice","dateOfBirth":"1990-01-01","address":"Example street","phone":"12345",
        "email":"alice@example.invalid","membershipType":"SINGLE","paymentMethod":"PAYPAL",
        "paymentDeclared":true,"consent":%s,"listed":true}
        """.formatted(consent); }
    @Test void publicReadRoutesWorkWithoutLogin() throws Exception {
        mvc.perform(get("/api/board-members")).andExpect(status().isOk());
        mvc.perform(get("/api/members")).andExpect(status().isOk());
        mvc.perform(get("/api/membership/settings")).andExpect(status().isOk());
    }
    @Test void privateApplicationsRequireAdmin() throws Exception {
        mvc.perform(get("/api/admin/members")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/members").with(user("visitor").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/members").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/membership/settings").with(user("visitor").roles("USER"))).andExpect(status().isForbidden());
    }
    @Test void acceptsPublicApplicationButRequiresConsentAndPastDate() throws Exception {
        Member m=new Member(); m.setId(1L); m.setAnnualFee(new BigDecimal("25.00"));
        when(service.apply(any())).thenReturn(m);
        mvc.perform(post("/api/membership/applications").contentType("application/json").content(application(true))).andExpect(status().isCreated()).andExpect(jsonPath("$.annualFee").value(25));
        mvc.perform(post("/api/membership/applications").contentType("application/json").content(application(false))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/membership/applications").contentType("application/json").content(application(true).replace("1990-01-01","2990-01-01"))).andExpect(status().isBadRequest());
        verify(service,times(1)).apply(any());
    }
    @Test void rejectsInvalidPartnerDateAndEmail() throws Exception {
        String couple = application(true).replace("SINGLE", "COUPLE");
        mvc.perform(post("/api/membership/applications").contentType("application/json")
            .content(couple.replace("\"name\":\"Alice\"", "\"name\":\"Alice\",\"partnerDateOfBirth\":\"2990-01-01\"")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/membership/applications").contentType("application/json")
            .content(couple.replace("\"name\":\"Alice\"", "\"name\":\"Alice\",\"partnerEmail\":\"invalid-email\"")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void visibilitySelectionIsMandatory() throws Exception {
        mvc.perform(post("/api/membership/applications").contentType("application/json")
            .content(application(true).replace(",\"listed\":true", ""))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void adminCanSaveBoardMemberThroughMultipartPut() throws Exception {
        var saved = new com.bangla.karneval.model.BoardMember();
        saved.setId(2); saved.setName("Updated Name"); saved.setDesignation("Secretary");
        when(board.save(2,"Updated Name","Secretary",null,false)).thenReturn(saved);
        mvc.perform(multipart(org.springframework.http.HttpMethod.PUT,"/api/admin/board-members/2")
            .param("name","Updated Name").param("designation","Secretary").param("removeImage","false")
            .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.name").value("Updated Name"));
        verify(board).save(2,"Updated Name","Secretary",null,false);
    }
}
