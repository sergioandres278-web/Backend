package com.agencytrip.agencytripapi.application.service;

import com.agencytrip.agencytripapi.application.dto.FacebookUserPayload;
import com.agencytrip.agencytripapi.application.dto.GoogleUserPayload;
import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.*;
import com.agencytrip.agencytripapi.domain.model.*;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;

public class UserService {
    private final TravelRepositoryPort repo;
    private final PasswordHasherPort hasher;
    private final TokenPort tokens;
    private final GoogleTokenVerifierPort googleVerifier;
    private final FacebookTokenVerifierPort facebookVerifier;

    public UserService(TravelRepositoryPort repo, PasswordHasherPort hasher, TokenPort tokens) {
        this(repo, hasher, tokens, null, null);
    }

    public UserService(TravelRepositoryPort repo, PasswordHasherPort hasher, TokenPort tokens, GoogleTokenVerifierPort googleVerifier) {
        this(repo, hasher, tokens, googleVerifier, null);
    }

    public UserService(TravelRepositoryPort repo, PasswordHasherPort hasher, TokenPort tokens, GoogleTokenVerifierPort googleVerifier, FacebookTokenVerifierPort facebookVerifier) {
        this.repo = repo;
        this.hasher = hasher;
        this.tokens = tokens;
        this.googleVerifier = googleVerifier;
        this.facebookVerifier = facebookVerifier;
    }

    public User register(Requests.Register r) {
        if (repo.emailExists(r.correo(), null)) {
            throw CatalogService.bad("El correo ya está registrado.");
        }
        User u = new User();
        u.setNombre(r.nombre());
        u.setApellido(r.apellido());
        u.setCorreo(r.correo());
        u.setPassword(hasher.hash(r.password()));
        u.setIdRol(2);
        u.setFechaRegistro(LocalDateTime.now());
        u.setEstado(true);
        return repo.saveUser(u);
    }

    public LoginResult login(Requests.Login r) {
        User u = repo.findUserByEmail(r.correo())
                .filter(User::isEstado)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        if (!hasher.matches(r.password(), u.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        }
        String role = u.getRol() == null ? "CLIENTE" : u.getRol().getNombre();
        return new LoginResult(tokens.create(u.getIdUsuario(), u.getNombre(), u.getCorreo(), role), u, role);
    }

    public LoginResult loginWithGoogle(Requests.GoogleOAuthLogin r) {
        if (googleVerifier == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticación con Google no disponible.");
        }
        GoogleUserPayload payload = googleVerifier.verify(r.idToken());

        Optional<SocialAccount> socialOpt = repo.findSocialAccount("GOOGLE", payload.sub());
        if (socialOpt.isPresent()) {
            SocialAccount sa = socialOpt.get();
            User u = sa.getUsuario() != null ? sa.getUsuario() : repo.findUserById(sa.getIdUsuario()).orElse(null);
            if (u == null || !u.isEstado()) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario inactivo o no encontrado.");
            }
            String role = u.getRol() == null ? "CLIENTE" : u.getRol().getNombre();
            return new LoginResult(tokens.create(u.getIdUsuario(), u.getNombre(), u.getCorreo(), role), u, role);
        }

        // Si no está vinculado socialmente pero el correo ya existe en usuarios tradicionales:
        if (repo.emailExists(payload.email(), null)) {
            throw new BusinessException(HttpStatus.CONFLICT, "El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña.");
        }

        // Nuevo usuario social: SIEMPRE rol CLIENTE (idRol = 2), nunca ADMINISTRADOR
        String nombre = (payload.givenName() != null && !payload.givenName().isBlank())
                ? payload.givenName()
                : ((payload.name() != null && !payload.name().isBlank()) ? payload.name() : "Usuario");
        String apellido = (payload.familyName() != null && !payload.familyName().isBlank())
                ? payload.familyName()
                : "Google";

        User newUser = new User();
        newUser.setNombre(nombre);
        newUser.setApellido(apellido);
        newUser.setCorreo(payload.email().toLowerCase().trim());
        newUser.setPassword(hasher.hash(UUID.randomUUID().toString()));
        newUser.setIdRol(2); // Rol CLIENTE estrictamente
        newUser.setFechaRegistro(LocalDateTime.now());
        newUser.setEstado(true);

        User savedUser = repo.saveUser(newUser);
        repo.findRole(2).ifPresent(savedUser::setRol);

        SocialAccount sa = new SocialAccount();
        sa.setIdUsuario(savedUser.getIdUsuario());
        sa.setProveedor("GOOGLE");
        sa.setIdProveedor(payload.sub());
        sa.setCorreoProveedor(payload.email().toLowerCase().trim());
        sa.setFechaVinculacion(LocalDateTime.now());
        repo.saveSocialAccount(sa);

        String token = tokens.create(savedUser.getIdUsuario(), savedUser.getNombre(), savedUser.getCorreo(), "CLIENTE");
        return new LoginResult(token, savedUser, "CLIENTE");
    }

    public LoginResult loginWithFacebook(Requests.FacebookOAuthLogin r) {
        if (facebookVerifier == null) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticación con Facebook no disponible.");
        }
        FacebookUserPayload payload = facebookVerifier.verify(r.accessToken());

        Optional<SocialAccount> socialOpt = repo.findSocialAccount("FACEBOOK", payload.id());
        if (socialOpt.isPresent()) {
            SocialAccount sa = socialOpt.get();
            User u = sa.getUsuario() != null ? sa.getUsuario() : repo.findUserById(sa.getIdUsuario()).orElse(null);
            if (u == null || !u.isEstado()) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario inactivo o no encontrado.");
            }
            String role = u.getRol() == null ? "CLIENTE" : u.getRol().getNombre();
            return new LoginResult(tokens.create(u.getIdUsuario(), u.getNombre(), u.getCorreo(), role), u, role);
        }

        // Si no está vinculado socialmente pero el correo ya existe en usuarios tradicionales:
        if (repo.emailExists(payload.email(), null)) {
            throw new BusinessException(HttpStatus.CONFLICT, "El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña.");
        }

        // Nuevo usuario social: SIEMPRE rol CLIENTE (idRol = 2), nunca ADMINISTRADOR
        String nombre = (payload.firstName() != null && !payload.firstName().isBlank())
                ? payload.firstName()
                : ((payload.name() != null && !payload.name().isBlank()) ? payload.name() : "Usuario");
        String apellido = (payload.lastName() != null && !payload.lastName().isBlank())
                ? payload.lastName()
                : "Facebook";

        User newUser = new User();
        newUser.setNombre(nombre);
        newUser.setApellido(apellido);
        newUser.setCorreo(payload.email().toLowerCase().trim());
        newUser.setPassword(hasher.hash(UUID.randomUUID().toString()));
        newUser.setIdRol(2); // Rol CLIENTE estrictamente
        newUser.setFechaRegistro(LocalDateTime.now());
        newUser.setEstado(true);

        User savedUser = repo.saveUser(newUser);
        repo.findRole(2).ifPresent(savedUser::setRol);

        SocialAccount sa = new SocialAccount();
        sa.setIdUsuario(savedUser.getIdUsuario());
        sa.setProveedor("FACEBOOK");
        sa.setIdProveedor(payload.id());
        sa.setCorreoProveedor(payload.email().toLowerCase().trim());
        sa.setFechaVinculacion(LocalDateTime.now());
        repo.saveSocialAccount(sa);

        String token = tokens.create(savedUser.getIdUsuario(), savedUser.getNombre(), savedUser.getCorreo(), "CLIENTE");
        return new LoginResult(token, savedUser, "CLIENTE");
    }

    public List<User> users() {
        return repo.findActiveUsers();
    }

    public List<Role> roles() {
        return repo.findRoles();
    }

    public User update(int id, Requests.UserUpdate r) {
        return update(id, r, null);
    }

    public User update(int id, Requests.UserUpdate r, Integer authenticatedUserId) {
        User u = repo.findUserById(id).orElseThrow(() -> CatalogService.notFound("El usuario no existe."));
        if (repo.emailExists(r.correo(), id)) {
            throw CatalogService.bad("El correo ya está registrado por otro usuario.");
        }
        Role role = repo.findRole(r.idRol()).orElseThrow(() -> CatalogService.bad("El rol no existe."));
        if (authenticatedUserId != null && authenticatedUserId.equals(id)) {
            if (!"ADMINISTRADOR".equalsIgnoreCase(role.getNombre())) {
                throw CatalogService.bad("No puedes revocar tu propio rol de ADMINISTRADOR.");
            }
        }
        u.setNombre(r.nombre());
        u.setApellido(r.apellido());
        u.setCorreo(r.correo());
        u.setIdRol(r.idRol());
        u.setRol(role);
        return repo.saveUser(u);
    }

    public void disable(int id) {
        disable(id, null);
    }

    public void disable(int id, Integer authenticatedUserId) {
        if (authenticatedUserId != null && authenticatedUserId.equals(id)) {
            throw CatalogService.bad("Por seguridad, no puedes desactivar tu propia cuenta administradora.");
        }
        User u = repo.findUserById(id).orElseThrow(() -> CatalogService.notFound("El usuario no existe."));
        if (!u.isEstado()) {
            throw CatalogService.bad("El usuario ya está desactivado.");
        }
        u.setEstado(false);
        repo.saveUser(u);
    }

    public record LoginResult(String token, User user, String role) {}
}
