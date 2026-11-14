package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
    @DisplayName("GET /api/Destinos es público y retorna 200 con destinos activos")
    void destinationsListIsPublic200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setPais("Colombia");
        d.setEstado(true);
        when(catalogService.destinations()).thenReturn(List.of(d));

        mockMvc.perform(get("/api/Destinos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Cartagena"))
                .andExpect(jsonPath("$[0].estado").value(true));
    }

    @Test
    @DisplayName("GET /api/Destinos?todos=true retorna todos los destinos (activos e inactivos)")
    void destinationsWithTodosParamReturnsAll() throws Exception {
        Destination d1 = new Destination();
        d1.setIdDestino(1);
        d1.setNombre("Cartagena");
        d1.setPais("Colombia");
        d1.setEstado(false);

        Destination d2 = new Destination();
        d2.setIdDestino(2);
        d2.setNombre("Medellín");
        d2.setPais("Colombia");
        d2.setEstado(true);

        when(catalogService.allDestinations()).thenReturn(List.of(d1, d2));

        mockMvc.perform(get("/api/Destinos").param("todos", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Cartagena"))
                .andExpect(jsonPath("$[0].estado").value(false))
                .andExpect(jsonPath("$[1].nombre").value("Medellín"))
                .andExpect(jsonPath("$[1].estado").value(true));
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
    @DisplayName("PUT /api/Destinos/{id}/estado permite a ADMINISTRADOR cambiar estado a inactivo")
    void changeDestinationStatusDeactivateAdminReturns200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setPais("Colombia");
        d.setEstado(false);

        when(catalogService.changeDestinationStatus(eq(1), eq(false))).thenReturn(d);

        mockMvc.perform(put("/api/Destinos/1/estado")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Destino desactivado correctamente."))
                .andExpect(jsonPath("$.destino.idDestino").value(1))
                .andExpect(jsonPath("$.destino.estado").value(false));
    }

    @Test
    @DisplayName("PUT /api/Destinos/{id}/estado permite a ADMINISTRADOR cambiar estado a activo")
    void changeDestinationStatusActivateAdminReturns200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setPais("Colombia");
        d.setEstado(true);

        when(catalogService.changeDestinationStatus(eq(1), eq(true))).thenReturn(d);

        mockMvc.perform(put("/api/Destinos/1/estado")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Destino activado correctamente."))
                .andExpect(jsonPath("$.destino.idDestino").value(1))
                .andExpect(jsonPath("$.destino.estado").value(true));
    }

    @Test
    @DisplayName("PUT /api/Destinos/{id}/activar permite a ADMINISTRADOR reactivar destino")
    void activateDestinationAdminReturns200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setPais("Colombia");
        d.setEstado(true);

        when(catalogService.changeDestinationStatus(eq(1), eq(true))).thenReturn(d);

        mockMvc.perform(put("/api/Destinos/1/activar")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Destino activado correctamente."))
                .andExpect(jsonPath("$.destino.estado").value(true));
    }

    @Test
    @DisplayName("PUT /api/Destinos/{id}/estado deniega acceso con 403 a rol CLIENTE")
    void changeDestinationStatusReturns403ForClient() throws Exception {
        mockMvc.perform(put("/api/Destinos/1/estado")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"))
                        .jwt(j -> j.claim("userId", 2).claim("email", "cliente@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/Destinos/{id}/estado deniega acceso con 401 a anónimo")
    void changeDestinationStatusReturns401ForAnonymous() throws Exception {
        mockMvc.perform(put("/api/Destinos/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/Destinos/{id}/estado retorna 404 cuando el destino no existe")
    void changeDestinationStatusReturns404WhenNotFound() throws Exception {
        when(catalogService.changeDestinationStatus(eq(999), anyBoolean()))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "El destino no existe."));

        mockMvc.perform(put("/api/Destinos/999/estado")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/Destinos/{id} desactiva destino y retorna 200 para ADMINISTRADOR")
    void deleteDestinationDeactivatesAndReturns200() throws Exception {
        Destination d = new Destination();
        d.setIdDestino(1);
        d.setNombre("Cartagena");
        d.setEstado(false);
        when(catalogService.changeDestinationStatus(eq(1), eq(false))).thenReturn(d);

        mockMvc.perform(delete("/api/Destinos/1")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        .jwt(j -> j.claim("userId", 1).claim("email", "admin@test.co"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Destino desactivado correctamente."));
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
