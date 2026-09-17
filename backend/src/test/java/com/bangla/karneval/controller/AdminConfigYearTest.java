package com.bangla.karneval.controller;

import com.bangla.karneval.model.EventConfig;
import com.bangla.karneval.repository.EventConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminConfigYearTest {
    @Mock EventConfigRepository repository;
    @InjectMocks AdminConfigController controller;

    @Test void repeatedYearReturnsExistingRowAndPreservesMembershipSettings() {
        var existing=new EventConfig(); existing.setId(7); existing.setEventYear(2026);
        existing.setMembershipCoupleFee(new BigDecimal("30.00")); existing.setMembershipBenefits("Voting");
        existing.setRegistrationEnabled(false);
        when(repository.findByEventYear(2026)).thenReturn(Optional.of(existing));
        assertSame(existing,controller.addYear(Map.of("eventYear",2026)).getBody());
        assertSame(existing,controller.addYear(Map.of("eventYear",2026)).getBody());
        assertEquals("Voting",existing.getMembershipBenefits());
        assertEquals(new BigDecimal("30.00"),existing.getMembershipCoupleFee());
        assertFalse(existing.getRegistrationEnabled());
        verify(repository,never()).save(any()); verify(repository,never()).existsById(any());
    }
    @Test void newYearIsSavedUsingTheRequestedYear() {
        when(repository.findByEventYear(2027)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        assertEquals(2027,controller.addYear(Map.of("eventYear",2027)).getBody().getEventYear());
        verify(repository).save(argThat(config->config.getEventYear()==2027));
    }
    @Test void missingYearReturnsBadRequestWithoutDatabaseAccess() {
        assertEquals(400,controller.addYear(Map.of()).getStatusCode().value());
        verifyNoInteractions(repository);
    }
}
