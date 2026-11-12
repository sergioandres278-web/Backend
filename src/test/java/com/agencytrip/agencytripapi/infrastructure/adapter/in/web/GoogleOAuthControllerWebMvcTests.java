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
import com.agencytrip.agencytripapi.application.service.BusinessException;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.User;

@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class GoogleOAuthControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private com.agencytrip.agencytripapi.application.port.out.GoogleTokenVerifierPort googleTokenVerifier;

    @MockitoBean
    private com.agencytrip.agencytripapi.application.port.out.FacebookTokenVerifierPort facebookTokenVerifier;

    @Test
    @DisplayName("POST /api/Usuarios/oauth/google: Devuelve 200 con formato de respuesta idéntico al login tradicional")
    void googleOAuthReturns200WithLoginResponseShape() throws Exception {
        User u = new User();
        u.setIdUsuario(77);
        u.setNombre("Lucia");
        u.setApellido("Fernandez");
        u.setCorreo("lucia@gmail.com");
        u.setIdRol(2);
        Role r = new Role();
        r.setIdRol(2);
        r.setNombre("CLIENTE");
        u.setRol(r);

        UserService.LoginResult loginResult = new UserService.LoginResult("jwt-agencytrip-xyz", u, "CLIENTE");
        when(userService.loginWithGoogle(any(Requests.GoogleOAuthLogin.class))).thenReturn(loginResult);

        mockMvc.perform(post("/api/Usuarios/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"valid-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso."))
                .andExpect(jsonPath("$.token").value("jwt-agencytrip-xyz"))
                .andExpect(jsonPath("$.usuario.idUsuario").value(77))
                .andExpect(jsonPath("$.usuario.nombre").value("Lucia"))
                .andExpect(jsonPath("$.usuario.apellido").value("Fernandez"))
                .andExpect(jsonPath("$.usuario.correo").value("lucia@gmail.com"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/google: Devuelve 400 si idToken está vacío")
    void emptyIdTokenReturns400() throws Exception {
        mockMvc.perform(post("/api/Usuarios/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/google: Devuelve 401 si el token es inválido o expirado")
    void invalidTokenReturns401() throws Exception {
        when(userService.loginWithGoogle(any()))
                .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "El token de Google es inválido o ha expirado."));

        mockMvc.perform(post("/api/Usuarios/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"expired-or-bad-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("El token de Google es inválido o ha expirado."));
    }

    @Test
    @DisplayName("POST /api/Usuarios/oauth/google: Devuelve 409 si existe conflicto con cuenta tradicional")
    void emailConflictReturns409() throws Exception {
        when(userService.loginWithGoogle(any()))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña."));

        mockMvc.perform(post("/api/Usuarios/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token-conflict\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado con una cuenta tradicional. Inicia sesión con tu contraseña."));
    }
}
