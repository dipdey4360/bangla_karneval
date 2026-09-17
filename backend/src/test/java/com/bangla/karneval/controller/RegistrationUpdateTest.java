package com.bangla.karneval.controller;

import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.RegistrationRepository;
import com.bangla.karneval.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RegistrationUpdateTest {
    @Mock RegistrationRepository repository;
    @Mock EmailService emailService;
    @InjectMocks RegistrationService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        var controller=new AdminRegistrationController();
        ReflectionTestUtils.setField(controller,"registrationService",service);
        mvc=MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new MembershipExceptionHandler()).build();
    }
    Registration existing() {
        var reg=new Registration(); reg.setId(1L); reg.setPaymentStatus(PaymentStatus.CONFIRMED);
        reg.setEmail("old@example.invalid"); reg.setPhone("123"); reg.setAddress("Old address");
        when(repository.findById(1L)).thenReturn(Optional.of(reg));
        when(repository.save(reg)).thenReturn(reg); return reg;
    }
    @Test void contactOnlyUpdatePreservesConfirmedPayment() throws Exception {
        var reg=existing();
        mvc.perform(put("/api/admin/registrations/1").contentType("application/json")
            .content("{\"phone\":\"456\",\"email\":\"new@example.invalid\",\"address\":\"New address\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
        assertEquals("456",reg.getPhone()); assertEquals("new@example.invalid",reg.getEmail()); assertEquals("New address",reg.getAddress());
        verify(emailService).sendRegistrationStatusEmail(reg,"");
    }
    @Test void explicitNullAndEmptyUpdatePreserveExistingValues() throws Exception {
        var reg=existing();
        for(String body:new String[]{"{}","{\"paymentStatus\":null,\"phone\":null,\"email\":null,\"address\":null}"}) {
            mvc.perform(put("/api/admin/registrations/1").contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("CONFIRMED"));
        }
        assertEquals("123",reg.getPhone()); assertEquals("Old address",reg.getAddress());
    }
    @Test void explicitStatusChangeAndClearingAddressStillWork() throws Exception {
        var reg=existing();
        mvc.perform(put("/api/admin/registrations/1").contentType("application/json")
            .content("{\"paymentStatus\":\"OVERDUE\",\"address\":\"\",\"adminNote\":\"Please contact us\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.paymentStatus").value("OVERDUE"));
        assertEquals("",reg.getAddress()); verify(emailService).sendRegistrationStatusEmail(reg,"Please contact us");
    }
    @Test void invalidStatusOrEmailIsRejectedBeforeSaving() throws Exception {
        for(String body:new String[]{"{\"paymentStatus\":\"UNKNOWN\"}","{\"email\":\"invalid\"}"}) {
            mvc.perform(put("/api/admin/registrations/1").contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        }
        verifyNoInteractions(repository,emailService);
    }
}
