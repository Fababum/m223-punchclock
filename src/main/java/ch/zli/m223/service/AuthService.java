package ch.zli.m223.service;

import java.time.Duration;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import ch.zli.m223.model.ApplicationUser;
import ch.zli.m223.model.Role;
import ch.zli.m223.repository.ApplicationUserRepository;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;

@ApplicationScoped
public class AuthService {
    @Inject
    ApplicationUserRepository userRepository;
    
    public  String login(String email, String password) {
        ApplicationUser user = userRepository.findByEmail(email);
        if(user == null || !BcryptUtil.matches(password, user.getPassword())) {
            return null;
        }
        return Jwt.upn(user.getEmail()).issuer("https://example.com/issuer").groups(Set.of(user.getRole().name())).expiresIn(Duration.ofHours(1)).sign();

    }

    @Transactional
    public ApplicationUser register(String email, String password, Role role) {
        if (userRepository.findByEmail(email) != null) {
            return null;
        }
        ApplicationUser user = new ApplicationUser();
        user.setEmail(email);
        user.setPassword(BcryptUtil.bcryptHash(password));
        user.setRole(role);
        userRepository.persist(user);
        return user;
    }
}