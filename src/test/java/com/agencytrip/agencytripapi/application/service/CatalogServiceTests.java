package com.agencytrip.agencytripapi.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
