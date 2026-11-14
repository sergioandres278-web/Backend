package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.port.out.FacebookTokenVerifierPort;
import com.agencytrip.agencytripapi.application.port.out.GoogleTokenVerifierPort;
import com.agencytrip.agencytripapi.application.service.BusinessException;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.User;

@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class FacebookOAuthControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private GoogleTokenVerifierPort googleTokenVerifier;

    @MockitoBean
    private FacebookTokenVerifierPort facebookTokenVerifier;

    @Test
    @DisplayName("POST /api/Usuarios/oauth/facebook: Devuelve 200 con formato idéntico al login tradicional")
    void facebookOAuthReturns200WithLoginResponseShape() throws Exception {
        User u = new User();
        u.setIdUsuario(88);
        u.setNombre("Mateo");
        u.setApellido("Morales");
        u.setCorreo("mateo@facebook.com");
        u.setIdRol(2);
        Role r = new Role();
        r.setIdRol(2);
        r.setNombre("CLIENTE");
        u.setRol(r);

        UserService.LoginResult loginResult = new UserService.LoginResult("jwt-facebook-token-xyz", u, "CLIENTE");
        when(userService.loginWithFacebook(any(Requests.FacebookOAuthLogin.class))).thenReturn(loginResult);

        mockMvc.perform(post("/api/Usuarios/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"valid-facebook-access-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso."))
                .andExpect(jsonPath("$.token").value("jwt-facebook-token-xyz"))
                .andExpect(jsonPath("$.usuario.idUsuario").value(88))
                .andExpect(jsonPath("$.usuario.nombre").value("Mateo"))
                .andExpect(jsonPath("$.usuario.apellido").value("Morales"))
                .andExpect(jsonPath("$.usuario.correo").value("mateo@facebook.com"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/facebook: Devuelve 400 si accessToken está vacío")
    void emptyAccessTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/Usuarios/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/facebook: Devuelve 401 si el token es inválido o expirado")
    void invalidTokenReturns401() throws Exception {
        when(userService.loginWithFacebook(any()))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Facebook es inválido o ha sido rechazado por Meta."));

        mockMvc.perform(post("/api/Usuarios/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"bad-or-expired-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El token de Facebook es inválido o ha sido rechazado por Meta."));
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/facebook: Devuelve 409 si existe conflicto con cuenta tradicional")
    void emailConflictReturns409() throws Exception {
        when(userService.loginWithFacebook(any()))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña."));

        mockMvc.perform(post("/api/Usuarios/oauth/facebook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accessToken\":\"token-conflict\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña."));
    }
}

