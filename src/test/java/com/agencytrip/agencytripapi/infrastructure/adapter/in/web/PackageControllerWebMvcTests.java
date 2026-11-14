package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.PackageService;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;

@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class PackageControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PackageService packageService;

    @Test
    @DisplayName("GET /api/Paquetes es público y retorna 200 sin autenticación")
    void packagesListIsPublic200() throws Exception {
        PackageTrip p = new PackageTrip();
        p.setIdPaquete(1);
        p.setNombre("Paquete Caribe");
        p.setPrecio(new BigDecimal("1500000"));
        p.setCupos(10);
        p.setEstado(true);

        when(packageService.list()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/Paquetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Paquete Caribe"));
    }

    @Test
    @DisplayName("POST /api/Paquetes permite crear paquete con rol ADMINISTRADOR y retorna 200")
    void createPackageReturns200ForAdminRole() throws Exception {
        PackageTrip p = new PackageTrip();
        p.setIdPaquete(2);
        p.setNombre("Paquete Eje Cafetero");
        p.setPrecio(new BigDecimal("800000"));
        p.setCupos(8);
        p.setEstado(true);

        when(packageService.create(any(Requests.PackageTrip.class))).thenReturn(p);

        String json = "{"
                + "\"nombre\":\"Paquete Eje Cafetero\","
                + "\"descripcion\":\"Tour cafetero\","
                + "\"precio\":800000,"
                + "\"fechaSalida\":\"" + LocalDateTime.now().plusDays(5) + "\","
                + "\"fechaRegreso\":\"" + LocalDateTime.now().plusDays(10) + "\","
                + "\"cupos\":8,"
                + "\"idDestino\":1"
                + "}";

        mockMvc.perform(post("/api/Paquetes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Paquete creado correctamente."))
                .andExpect(jsonPath("$.paquete.nombre").value("Paquete Eje Cafetero"));
    }

    @Test
    @DisplayName("POST /api/Paquetes deniega acceso con 403 a rol CLIENTE")
    void createPackageReturns403ForClientRole() throws Exception {
        String json = "{"
                + "\"nombre\":\"Paquete Eje Cafetero\","
                + "\"descripcion\":\"Tour cafetero\","
                + "\"precio\":800000,"
                + "\"fechaSalida\":\"" + LocalDateTime.now().plusDays(5) + "\","
                + "\"fechaRegreso\":\"" + LocalDateTime.now().plusDays(10) + "\","
                + "\"cupos\":8,"
                + "\"idDestino\":1"
                + "}";

        mockMvc.perform(post("/api/Paquetes")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 2).claim("email", "cliente@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/Paquetes deniega acceso con 401 a petición anónima")
    void createPackageReturns401ForAnonymous() throws Exception {
        String json = "{"
                + "\"nombre\":\"Paquete Eje Cafetero\","
                + "\"descripcion\":\"Tour cafetero\","
                + "\"precio\":800000,"
                + "\"fechaSalida\":\"" + LocalDateTime.now().plusDays(5) + "\","
                + "\"fechaRegreso\":\"" + LocalDateTime.now().plusDays(10) + "\","
                + "\"cupos\":8,"
                + "\"idDestino\":1"
                + "}";

        mockMvc.perform(post("/api/Paquetes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized());
    }
}
