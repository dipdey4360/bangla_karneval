package com.bangla.karneval.repository;

import com.bangla.karneval.model.PaymentStatus;
import com.bangla.karneval.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByPaymentStatus(PaymentStatus status);
    List<Registration> findByEventYear(Integer year);

    @Query("SELECT r FROM Registration r WHERE r.paymentStatus = 'PENDING' AND r.registeredAt < :cutoffDate")
    List<Registration> findOverduePayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT r FROM Registration r WHERE LOWER(r.primaryName) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Registration> searchByName(@Param("search") String search);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.eventYear = :year")
    Long countByYear(@Param("year") Integer year);

    @Query("SELECT SUM(r.calculatedAmount) FROM Registration r WHERE r.paymentStatus = 'CONFIRMED' AND r.eventYear = :year")
    BigDecimal sumRevenueByYear(@Param("year") Integer year);

    @Query("SELECT COUNT(r) FROM Registration r WHERE r.paymentStatus = :status")
    Long countByPaymentStatus(@Param("status") PaymentStatus status);
}
