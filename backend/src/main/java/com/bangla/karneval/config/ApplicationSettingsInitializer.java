package com.bangla.karneval.config;
import com.bangla.karneval.service.ApplicationSettingsService;
import com.bangla.karneval.service.ProgrammeFoundationMigration;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
@Component
public class ApplicationSettingsInitializer implements ApplicationRunner {
    private final ApplicationSettingsService settings;
    private final ProgrammeFoundationMigration migration;
    public ApplicationSettingsInitializer(ApplicationSettingsService settings, ProgrammeFoundationMigration migration) {
        this.settings=settings; this.migration=migration;
    }
    @Override public void run(ApplicationArguments args) throws Exception { settings.initialize(); migration.migrate(); }
}
