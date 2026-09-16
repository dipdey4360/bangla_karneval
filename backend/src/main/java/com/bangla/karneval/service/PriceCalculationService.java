package com.bangla.karneval.service;

import com.bangla.karneval.model.Registration;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class PriceCalculationService {

    /**
     * Calculates age in full years from date of birth to today.
     */
    public int calculateAge(LocalDate dateOfBirth) {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Primary registrant always pays full price regardless of age.
     * Additional participants under 18 are FREE.
     * Additional participants 18+ pay full price.
     */
    public BigDecimal calculateTotalAmount(LocalDate primaryDob,
                                           List<LocalDate> additionalDobs,
                                           BigDecimal pricePerPerson) {
        BigDecimal total = pricePerPerson; // primary always pays

        for (LocalDate dob : additionalDobs) {
            int age = calculateAge(dob);
            if (age >= 18) {
                total = total.add(pricePerPerson);
            }
            // children under 18 are free — add nothing
        }
        return total;
    }

    /**
     * Age group breakdown for dashboard stats.
     * Calculates age from date of birth at runtime.
     */
    public AgeGroupStats getAgeGroupStats(List<Registration> registrations) {
        int children = 0, adults = 0, seniors = 0;

        for (Registration reg : registrations) {
            if (reg.getPrimaryDateOfBirth() != null) {
                int age = calculateAge(reg.getPrimaryDateOfBirth());
                if      (age < 18) children++;
                else if (age < 60) adults++;
                else               seniors++;
            }

            if (reg.getAdditionalParticipants() != null) {
                for (var ap : reg.getAdditionalParticipants()) {
                    if (ap.getDateOfBirth() != null) {
                        int age = calculateAge(ap.getDateOfBirth());
                        if      (age < 18) children++;
                        else if (age < 60) adults++;
                        else               seniors++;
                    }
                }
            }
        }
        return new AgeGroupStats(children, adults, seniors);
    }

    public static class AgeGroupStats {
        private final int children;
        private final int adults;
        private final int seniors;

        public AgeGroupStats(int children, int adults, int seniors) {
            this.children = children;
            this.adults   = adults;
            this.seniors  = seniors;
        }
        public int getChildren() { return children; }
        public int getAdults()   { return adults;   }
        public int getSeniors()  { return seniors;  }
    }
}
