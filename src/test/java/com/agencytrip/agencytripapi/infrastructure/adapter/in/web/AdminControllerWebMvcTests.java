package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.agencytrip.agencytripapi.application.service.UserService;
import com.agencytrip.agencytripapi.domain.model.Role;

@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class AdminControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /api/Admin/panel retorna 200 para usuario con rol ADMINISTRADOR")
    void panelReturns200ForAdminRole() throws Exception {
        mockMvc.perform(get("/api/Admin/panel")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("GET /api/Admin/panel retorna 403 para usuario con rol CLIENTE")
    void panelReturns403ForClientRole() throws Exception {
        mockMvc.perform(get("/api/Admin/panel")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 2).claim("email", "cliente@test.co"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/Admin/panel retorna 401 para solicitud anónima")
    void panelReturns401ForAnonymous() throws Exception {
        mockMvc.perform(get("/api/Admin/panel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/Roles retorna 200 y lista de roles para rol ADMINISTRADOR")
    void rolesReturns200ForAdminRole() throws Exception {
        Role r1 = new Role(); r1.setIdRol(1); r1.setNombre("ADMINISTRADOR");
        Role r2 = new Role(); r2.setIdRol(2); r2.setNombre("CLIENTE");
        when(userService.roles()).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/Roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$[1].nombre").value("CLIENTE"));
    }

    @Test
    @DisplayName("GET /api/Roles retorna 403 para rol CLIENTE")
    void rolesReturns403ForClientRole() throws Exception {
        mockMvc.perform(get("/api/Roles")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 2).claim("email", "cliente@test.co"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/Roles retorna 401 para solicitud anónima")
    void rolesReturns401ForAnonymous() throws Exception {
        mockMvc.perform(get("/api/Roles"))
                .andExpect(status().isUnauthorized());
    }
}
