package ch.zli.m223.controller;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import ch.zli.m223.dto.LoginDto;
import ch.zli.m223.model.ApplicationUser;
import ch.zli.m223.model.Role;
import ch.zli.m223.service.AuthService;

@Path("/auth")
@Tag(name = "Auth", description = "Authentication")
public class AuthController {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Login", description = "Returns a signed JWT.")
    public Response login(@Valid LoginDto dto) {
        String token = authService.login(dto.email, dto.password);
        if(token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.ok(token).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Register", description = "Creates a new user with User role.")
    public Response register(@Valid LoginDto dto) {
        ApplicationUser user = authService.register(dto.email, dto.password, Role.User);
        if(user == null) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.status(Response.Status.CREATED).build();
    }
}
