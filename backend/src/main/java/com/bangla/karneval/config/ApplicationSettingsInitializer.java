package com.bangla.karneval.config;
import com.bangla.karneval.service.ApplicationSettingsService;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
@Component
public class ApplicationSettingsInitializer implements ApplicationRunner {
    private final ApplicationSettingsService settings;
    public ApplicationSettingsInitializer(ApplicationSettingsService settings) { this.settings=settings; }
    @Override public void run(ApplicationArguments args) { settings.initialize(); }
}
