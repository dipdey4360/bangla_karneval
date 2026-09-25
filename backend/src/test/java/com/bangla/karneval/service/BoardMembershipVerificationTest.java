package com.bangla.karneval.service;

import com.bangla.karneval.model.*;
import com.bangla.karneval.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BoardMembershipVerificationTest {
    @Test void sixPositionIdsTransferToCurrentOccupantAndNeverFallBackToOrdinaryMembers() {
        var members=mock(MemberRepository.class);
        var board=mock(BoardMemberRepository.class);
        var settings=mock(ApplicationSettingsService.class);
        var config=new ApplicationSettings(); config.setMemberDiscountPercent(new BigDecimal("25"));
        when(settings.getSettings()).thenReturn(config);
        var service=new MembershipVerificationService(members,settings,board);
        for(int slot=1;slot<=6;slot++) {
            var occupant=new BoardMember(); occupant.setId(slot); occupant.setName("Original Member");
            when(board.findById(slot)).thenReturn(Optional.of(occupant));
            String id=String.format("BKM-%05d",slot);
            assertEquals(new BigDecimal("25"),service.verify(id.toLowerCase()," ORIGINAL  Member ").discountPercent());
            occupant.setName("New Member");
            assertThrows(ResponseStatusException.class,()->service.verify(id,"Original Member"));
            assertTrue(service.verify(id,"New Member").verified());
            assertThrows(ResponseStatusException.class,()->service.verify(id,"New Member Partner"));
            when(board.findById(slot)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class,()->service.verify(id,"New Member"));
        }
        verifyNoInteractions(members);
    }
}
