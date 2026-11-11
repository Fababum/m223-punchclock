package ch.zli.m223.service;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import ch.zli.m223.model.Role;

@ApplicationScoped
public class StartupService {
    @Inject
    AuthService authService;

    void onStart(@Observes StartupEvent ev) {
        authService.register("admin@example.com", "admin123", Role.Admin);
    }
}
