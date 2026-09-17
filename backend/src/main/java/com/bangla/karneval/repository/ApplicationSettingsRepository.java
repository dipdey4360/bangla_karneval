package com.bangla.karneval.repository;
import com.bangla.karneval.model.ApplicationSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface ApplicationSettingsRepository extends JpaRepository<ApplicationSettings,Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ApplicationSettings s where s.id = 1")
    Optional<ApplicationSettings> lockSettings();
}
