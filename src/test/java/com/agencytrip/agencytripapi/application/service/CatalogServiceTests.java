package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class CatalogServiceTests {

    @Test
    void createsAnActiveDestinationWithTheRequestedData() {
        TravelRepositoryPort repository = mock(TravelRepositoryPort.class);
        when(repository.saveDestination(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CatalogService service = new CatalogService(repository);

        Destination destination = service.createDestination(
                new Requests.Destination("Cartagena", "Colombia", "Ciudad amurallada", "https://example.test/cartagena.jpg"));

        assertEquals("Cartagena", destination.getNombre());
        assertEquals("Colombia", destination.getPais());
        assertEquals("Ciudad amurallada", destination.getDescripcion());
        assertTrue(destination.isEstado());
    }

    @Test
    void rejectsHotelCategoriesOutsideTheSupportedRange() {
        CatalogService service = new CatalogService(mock(TravelRepositoryPort.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createHotel(new Requests.Hotel("Hotel", "Calle 1", "Bogotá", 6, "Prueba")));

        assertEquals("La categoría debe estar entre 1 y 5.", exception.getMessage());
    }

    @Test
    void rejectsHotelCategoryBelowOne() {
        CatalogService service = new CatalogService(mock(TravelRepositoryPort.class));
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.createHotel(new Requests.Hotel("Hotel", "Calle 1", "Bogotá", 0, "Prueba")));
        assertEquals("La categoría debe estar entre 1 y 5.", exception.getMessage());
    }

    @Test
    void rejectsUpdatingNonExistentDestination() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        when(repo.findDestination(99)).thenReturn(java.util.Optional.empty());
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class,
                () -> service.updateDestination(99, new Requests.Destination("Cartagena", "Colombia", "Desc", "url")));
    }

    @Test
    void rejectsDisablingNonExistentDestination() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        when(repo.findDestination(99)).thenReturn(java.util.Optional.empty());
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class, () -> service.disableDestination(99));
    }

    @Test
    void rejectsUpdatingDeactivatedHotel() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Hotel h = new com.agencytrip.agencytripapi.domain.model.Hotel();
        h.setEstado(false);
        when(repo.findHotel(1)).thenReturn(java.util.Optional.of(h));
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class,
                () -> service.updateHotel(1, new Requests.Hotel("Hotel", "Calle 1", "Bogotá", 4, "Prueba")));
    }

    @Test
    void rejectsDisablingAlreadyDeactivatedHotel() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Hotel h = new com.agencytrip.agencytripapi.domain.model.Hotel();
        h.setEstado(false);
        when(repo.findHotel(1)).thenReturn(java.util.Optional.of(h));
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class, () -> service.disableHotel(1));
    }

    @Test
    void rejectsUpdatingDeactivatedTransport() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Transport t = new com.agencytrip.agencytripapi.domain.model.Transport();
        t.setEstado(false);
        when(repo.findTransport(1)).thenReturn(java.util.Optional.of(t));
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class,
                () -> service.updateTransport(1, new Requests.Transport("Aéreo", "Avianca", "Vuelo")));
    }

    @Test
    void rejectsDisablingAlreadyDeactivatedTransport() {
        TravelRepositoryPort repo = mock(TravelRepositoryPort.class);
        com.agencytrip.agencytripapi.domain.model.Transport t = new com.agencytrip.agencytripapi.domain.model.Transport();
        t.setEstado(false);
        when(repo.findTransport(1)).thenReturn(java.util.Optional.of(t));
        CatalogService service = new CatalogService(repo);
        assertThrows(BusinessException.class, () -> service.disableTransport(1));
    }
}

