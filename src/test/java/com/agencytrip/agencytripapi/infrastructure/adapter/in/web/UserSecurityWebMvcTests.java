package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.BusinessException;
import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.User;

/**
 * Pruebas de seguridad HTTP y autorización para los endpoints administrativos de usuarios:
 * PUT /api/Usuarios/{id} y DELETE /api/Usuarios/{id}.
 */
@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class UserSecurityWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("6. Una solicitud anónima no puede realizar operaciones administrativas (PUT / DELETE retornan 401)")
    void anonymousRequestCannotPerformAdminOperations() throws Exception {
        mockMvc.perform(delete("/api/Usuarios/5"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/Usuarios/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carlos\",\"apellido\":\"Perez\",\"correo\":\"carlos@example.com\",\"idRol\":2}"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).disable(any(Integer.class), any());
        verify(userService, never()).update(any(Integer.class), any(), any());
    }

    @Test
    @DisplayName("5. Un usuario con rol CLIENTE no puede realizar operaciones administrativas (retorna 403 Forbidden)")
    void clientRoleCannotPerformAdminOperations() throws Exception {
        mockMvc.perform(delete("/api/Usuarios/5")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 5).claim("email", "cliente@example.com"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/Usuarios/5")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 5).claim("email", "cliente@example.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Carlos\",\"apellido\":\"Perez\",\"correo\":\"carlos@example.com\",\"idRol\":2}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).disable(any(Integer.class), any());
        verify(userService, never()).update(any(Integer.class), any(), any());
    }

    @Test
    @DisplayName("1. Un administrador autenticado no puede auto-desactivarse vía API (retorna 400 Bad Request)")
    void adminCannotDeactivateThemselvesViaHttp() throws Exception {
        doThrow(new BusinessException(HttpStatus.BAD_REQUEST, "Por seguridad, no puedes desactivar tu propia cuenta administradora."))
                .when(userService).disable(eq(4), eq(4));

        mockMvc.perform(delete("/api/Usuarios/4")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 4).claim("email", "admin@agenciaviajes.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Por seguridad, no puedes desactivar tu propia cuenta administradora."));

        verify(userService).disable(4, 4);
    }

    @Test
    @DisplayName("2. Un administrador autenticado no puede auto-demoverse vía API (retorna 400 Bad Request)")
    void adminCannotDemoteThemselvesViaHttp() throws Exception {
        when(userService.update(eq(4), any(Requests.UserUpdate.class), eq(4)))
                .thenThrow(new BusinessException(HttpStatus.BAD_REQUEST, "No puedes revocar tu propio rol de ADMINISTRADOR."));

        mockMvc.perform(put("/api/Usuarios/4")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 4).claim("email", "admin@agenciaviajes.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Admin\",\"apellido\":\"Principal\",\"correo\":\"admin@agenciaviajes.com\",\"idRol\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes revocar tu propio rol de ADMINISTRADOR."));

        verify(userService).update(eq(4), any(Requests.UserUpdate.class), eq(4));
    }

    @Test
    @DisplayName("3. Un administrador puede actualizar sus propios campos manteniendo rol ADMINISTRADOR (retorna 200 OK)")
    void adminCanUpdateOwnAllowedFieldsViaHttp() throws Exception {
        User updated = new User();
        updated.setIdUsuario(4);
        updated.setNombre("AdminMod");
        updated.setApellido("PrincipalMod");
        updated.setCorreo("admin.nuevo@agenciaviajes.com");
        updated.setIdRol(1);
        Role role = new Role();
        role.setIdRol(1);
        role.setNombre("ADMINISTRADOR");
        updated.setRol(role);
        updated.setFechaRegistro(LocalDateTime.now());
        updated.setEstado(true);

        when(userService.update(eq(4), any(Requests.UserUpdate.class), eq(4))).thenReturn(updated);

        mockMvc.perform(put("/api/Usuarios/4")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 4).claim("email", "admin@agenciaviajes.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"AdminMod\",\"apellido\":\"PrincipalMod\",\"correo\":\"admin.nuevo@agenciaviajes.com\",\"idRol\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario actualizado correctamente."))
                .andExpect(jsonPath("$.usuario.nombre").value("AdminMod"))
                .andExpect(jsonPath("$.usuario.rol").value("ADMINISTRADOR"));

        verify(userService).update(eq(4), any(Requests.UserUpdate.class), eq(4));
    }

    @Test
    @DisplayName("4. Un administrador puede gestionar a otro usuario (actualizar y desactivar) vía API (retorna 200 OK)")
    void adminCanManageAnotherUserViaHttp() throws Exception {
        // Desactivar a otro usuario (ID 5)
        doNothing().when(userService).disable(eq(5), eq(4));

        mockMvc.perform(delete("/api/Usuarios/5")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 4).claim("email", "admin@agenciaviajes.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario desactivado correctamente."));

        verify(userService).disable(5, 4);

        // Actualizar a otro usuario (ID 5)
        User user5 = new User();
        user5.setIdUsuario(5);
        user5.setNombre("CarlosMod");
        user5.setApellido("Perez");
        user5.setCorreo("carlos.nuevo@example.com");
        user5.setIdRol(2);
        Role role = new Role();
        role.setIdRol(2);
        role.setNombre("CLIENTE");
        user5.setRol(role);
        user5.setFechaRegistro(LocalDateTime.now());
        user5.setEstado(true);

        when(userService.update(eq(5), any(Requests.UserUpdate.class), eq(4))).thenReturn(user5);

        mockMvc.perform(put("/api/Usuarios/5")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 4).claim("email", "admin@agenciaviajes.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"CarlosMod\",\"apellido\":\"Perez\",\"correo\":\"carlos.nuevo@example.com\",\"idRol\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario actualizado correctamente."));

        verify(userService).update(eq(5), any(Requests.UserUpdate.class), eq(4));
    }
}
