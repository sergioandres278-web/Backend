package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.agencytrip.agencytripapi.application.service.CatalogService;
import com.agencytrip.agencytripapi.domain.model.Destination;

@SpringBootTest(properties = {
    "agencytrip.jwt.secret=test_jwt_secret_key_with_at_least_32_characters_12345"
})
@AutoConfigureMockMvc
class CatalogControllerWebMvcTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    @DisplayName("GET /api/Destinos es público y retorna 200 sin autenticación")
    void destinationsListIsPublic200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setPais("Colombia");
        d.setEstado(true);
        when(catalogService.destinations()).thenReturn(List.of(d));

        mockMvc.perform(get("/api/Destinos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cartagena"));
    }

    @Test
    @DisplayName("POST /api/Destinos permite crear destino con rol ADMINISTRADOR y retorna 200")
    void createDestinationReturns200ForAdminRole() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(2);
        d.setNombre("Santa Marta");
        d.setPais("Colombia");
        d.setEstado(true);

        when(catalogService.createDestination(any(Requests.Destination.class))).thenReturn(d);

        mockMvc.perform(post("/api/Destinos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Santa Marta\",\"pais\":\"Colombia\",\"descripcion\":\"Playa\",\"imagen\":\"https://img.co/sm.jpg\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Destino creado correctamente."))
                .andExpect(jsonPath("$.destino.nombre").value("Santa Marta"));
    }

    @Test
    @DisplayName("POST /api/Destinos deniega acceso con 403 a rol CLIENTE")
    void createDestinationReturns403ForClientRole() throws Exception {
        mockMvc.perform(post("/api/Destinos")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 2).claim("email", "cliente@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Santa Marta\",\"pais\":\"Colombia\",\"descripcion\":\"Playa\",\"imagen\":\"https://img.co/sm.jpg\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/Destinos deniega acceso con 401 a petición anónima")
    void createDestinationReturns401ForAnonymous() throws Exception {
        mockMvc.perform(post("/api/Destinos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Santa Marta\",\"pais\":\"Colombia\",\"descripcion\":\"Playa\",\"imagen\":\"https://img.co/sm.jpg\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/Hoteles es público y retorna 200 sin autenticación")
    void hotelsListIsPublic200() throws Exception {
        when(catalogService.hotels()).thenReturn(List.of());

        mockMvc.perform(get("/api/Hoteles"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/Transportes es público y retorna 200 sin autenticación")
    void transportsListIsPublic200() throws Exception {
        when(catalogService.transports()).thenReturn(List.of());

        mockMvc.perform(get("/api/Transportes"))
                .andExpect(status().isOk());
    }
}
