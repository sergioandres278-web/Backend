package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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
class RegistrationControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/Registro registra un cliente exitosamente y retorna 200")
    void registersClientSuccessfullyWith200() throws Exception {
        User user = new User();
        user.setIdUsuario(10);
        user.setNombre("Carlos");
        user.setApellido("Gómez");
        user.setCorreo("carlos.gomez@test.co");
        user.setFechaRegistro(LocalDateTime.now());
        user.setEstado(true);
        Role role = new Role();
        role.setIdRol(2);
        role.setNombre("CLIENTE");
        user.setRol(role);

        when(userService.register(any(Requests.Register.class))).thenReturn(user);

        mockMvc.perform(post("/api/Registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carlos\",\"apellido\":\"Gómez\",\"correo\":\"carlos.gomez@test.co\",\"password\":\"ClaveSegura123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado correctamente."))
                .andExpect(jsonPath("$.usuario.nombre").value("Carlos"))
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"));
    }

    @Test
    @DisplayName("POST /api/Registro retorna 400 cuando el cuerpo no cumple las validaciones")
    void rejectsRegistrationWhenValidationFailsWith400() throws Exception {
        mockMvc.perform(post("/api/Registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"\",\"apellido\":\"\",\"correo\":\"correo-invalido\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/Registro retorna 400 cuando el correo ya está registrado")
    void handlesDuplicateEmailBusinessExceptionWith400() throws Exception {
        when(userService.register(any(Requests.Register.class)))
                .thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "El correo ya está registrado."));

        mockMvc.perform(post("/api/Registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carlos\",\"apellido\":\"Gómez\",\"correo\":\"duplicado@test.co\",\"password\":\"ClaveSegura123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El correo ya está registrado."));
    }
}
