package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.agencytrip.agencytripapi.application.service.CatalogService;
import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DestinationStatusIntegrationTests {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private TravelRepositoryPort travelRepository;

    @Test
    @DisplayName("Debe desactivar y activar lógicamente un destino preservando ID, datos y evitando duplicados")
    void cycleActivationDeactivationPreservesData() {
        // 1. Obtener o crear un destino de prueba 'Cartagena'
        List<Destination> todosIniciales = catalogService.allDestinations();
        Destination cartagena = todosIniciales.stream()
                .filter(d -> d.getNombre().toLowerCase().contains("cartagena"))
                .findFirst()
                .orElseGet(() -> {
                    Destination nuevo = new Destination();
                    nuevo.setNombre("Cartagena Test");
                    nuevo.setPais("Colombia");
                    nuevo.setDescripcion("Ciudad colonial y patrimonio histórico");
                    nuevo.setImagen("cartagena-test.jpg");
                    nuevo.setEstado(true);
                    return travelRepository.saveDestination(nuevo);
                });

        assertNotNull(cartagena);
        Integer idOriginal = cartagena.getIdDestino();
        String nombreOriginal = cartagena.getNombre();
        String paisOriginal = cartagena.getPais();
        String descOriginal = cartagena.getDescripcion();
        String imagenOriginal = cartagena.getImagen();

        // 2. DESACTIVAR DESTINO
        Destination desactivado = catalogService.changeDestinationStatus(idOriginal, false);

        assertNotNull(desactivado);
        assertFalse(desactivado.isEstado(), "El estado debe ser inactivo (false)");
        assertEquals(idOriginal, desactivado.getIdDestino(), "El ID debe ser exactamente el mismo");
        assertEquals(nombreOriginal, desactivado.getNombre(), "El nombre debe conservarse");
        assertEquals(paisOriginal, desactivado.getPais(), "El país debe conservarse");
        assertEquals(descOriginal, desactivado.getDescripcion(), "La descripción debe conservarse");
        assertEquals(imagenOriginal, desactivado.getImagen(), "La imagen debe conservarse");

        // 3. Verificar visibilidad pública vs administrativa
        List<Destination> activos = catalogService.destinations();
        boolean presenteEnPublico = activos.stream().anyMatch(d -> d.getIdDestino().equals(idOriginal));
        assertFalse(presenteEnPublico, "Un destino inactivo NO debe aparecer en el catálogo público");

        List<Destination> todos = catalogService.allDestinations();
        boolean presenteEnAdmin = todos.stream().anyMatch(d -> d.getIdDestino().equals(idOriginal));
        assertTrue(presenteEnAdmin, "Un destino inactivo SÍ debe seguir apareciendo en el catálogo administrativo");

        // Verificar que no se duplicó el registro
        long conteo = todos.stream().filter(d -> d.getIdDestino().equals(idOriginal)).count();
        assertEquals(1, conteo, "No debe existir más de un registro con el mismo ID");

        // 4. ACTIVAR DESTINO NUEVAMENTE
        Destination reactivado = catalogService.changeDestinationStatus(idOriginal, true);

        assertNotNull(reactivado);
        assertTrue(reactivado.isEstado(), "El estado debe volver a ser activo (true)");
        assertEquals(idOriginal, reactivado.getIdDestino(), "El ID debe mantenerse intacto");
        assertEquals(nombreOriginal, reactivado.getNombre(), "El nombre debe seguir intacto");

        // 5. Verificar que vuelve a estar visible en catálogo público
        List<Destination> activosReactivados = catalogService.destinations();
        boolean presenteEnPublicoTrasActivar = activosReactivados.stream().anyMatch(d -> d.getIdDestino().equals(idOriginal));
        assertTrue(presenteEnPublicoTrasActivar, "Un destino activado DEBE volver a estar disponible en el catálogo público");

        // 6. Repetibilidad del ciclo
        Destination ciclo2Desactivado = catalogService.changeDestinationStatus(idOriginal, false);
        assertFalse(ciclo2Desactivado.isEstado());
        Destination ciclo2Activado = catalogService.changeDestinationStatus(idOriginal, true);
        assertTrue(ciclo2Activado.isEstado());
        assertEquals(idOriginal, ciclo2Activado.getIdDestino());
    }

    @Test
    @DisplayName("Verificar inventario de destinos en base de datos de test")
    void checkTwelveDestinations() {
        List<Destination> todos = catalogService.allDestinations();
        System.out.println("=== INVENTARIO DE DESTINOS EN TEST ===");
        System.out.println("TOTAL_DESTINOS_TEST=" + todos.size());
        for (Destination d : todos) {
            System.out.println("DESTINO_TEST: ID=" + d.getIdDestino() + " | " + d.getNombre() + " (" + d.getPais() + ") | Estado=" + (d.isEstado() ? "ACTIVO" : "INACTIVO"));
        }
        System.out.println("======================================");
    }
}

