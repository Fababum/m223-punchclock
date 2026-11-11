package ch.zli.m223.repository;

import ch.zli.m223.model.ApplicationUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ApplicationUserRepository implements PanacheRepository<ApplicationUser> {
    public ApplicationUser findByEmail(String email) {
        return find ("email", email).firstResult();
    }
}
