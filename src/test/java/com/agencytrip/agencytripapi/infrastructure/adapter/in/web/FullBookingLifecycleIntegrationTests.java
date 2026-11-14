package com.agencytrip.agencytripapi.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.model.Hotel;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.model.Transport;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PRUEBA DE INTEGRACIÓN 1: CICLO COMPLETO DE RESERVA Y PAGO PSE
 *
 * Flujo probado de extremo a extremo:
 * 1. Registro de nuevo cliente (/api/Usuarios/registro).
 * 2. Autenticación y obtención de JWT (/api/Usuarios/login).
 * 3. Consulta de disponibilidad y cupos iniciales del paquete.
 * 4. Creación de reserva en estado PENDIENTE (/api/Reservas).
 * 5. Verificación de que los cupos no se descuentan prematuramente en estado PENDIENTE.
 * 6. Registro de pago con método PSE (/api/Pagos).
 * 7. Aprobación del pago e impacto en inventario.
 * 8. Transición de la reserva a estado CONFIRMADA.
 * 9. Descuento exacto de cupos disponibles según las personas reservadas.
 * 10. Consistencia final en base de datos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FullBookingLifecycleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TravelRepositoryPort travelRepository;

    private Destination testDestination;
    private Hotel testHotel;
    private Transport testTransport;

    @BeforeEach
    void setUp() {
        testDestination = travelRepository.findActiveDestinations().stream().findFirst()
                .orElseGet(() -> {
                    Destination d = new Destination();
                    d.setNombre("Destino Test");
                    d.setPais("Colombia");
                    d.setDescripcion("Destino de prueba");
                    d.setEstado(true);
                    return travelRepository.saveDestination(d);
                });
        testHotel = travelRepository.findActiveHotels().stream().findFirst()
                .orElseGet(() -> {
                    Hotel h = new Hotel();
                    h.setNombre("Hotel Test");
                    h.setCiudad("Test");
                    h.setCategoria(4);
                    h.setEstado(true);
                    return travelRepository.saveHotel(h);
                });
        testTransport = travelRepository.findActiveTransports().stream().findFirst()
                .orElseGet(() -> {
                    Transport t = new Transport();
                    t.setTipo("AVIÓN");
                    t.setEmpresa("AeroTest");
                    t.setEstado(true);
                    return travelRepository.saveTransport(t);
                });
    }

    private PackageTrip createTestPackage(int cupos, BigDecimal precio) {
        PackageTrip pkg = new PackageTrip();
        pkg.setNombre("LIFECYCLE_PKG_" + UUID.randomUUID().toString().substring(0, 8));
        pkg.setDescripcion("Paquete de prueba para ciclo completo de reserva y pago PSE");
        pkg.setPrecio(precio);
        pkg.setFechaSalida(LocalDateTime.now().plusDays(15));
        pkg.setFechaRegreso(LocalDateTime.now().plusDays(20));
        pkg.setCupos(cupos);
        pkg.setIdDestino(testDestination.getIdDestino());
        pkg.setIdHotel(testHotel.getIdHotel());
        pkg.setIdTransporte(testTransport.getIdTransporte());
        pkg.setEstado(true);

        return travelRepository.savePackage(pkg);
    }

    @Test
    @DisplayName("Ciclo completo E2E: Registro -> Login JWT -> Disponibilidad -> Reserva PENDIENTE -> Pago PSE -> CONFIRMADA -> Descuento de cupos y consistencia")
    void fullBookingLifecyclePSEPaymentApprovalAndQuotaDecrement() throws Exception {
        final int initialCupos = 10;
        final int personsToBook = 2;
        final BigDecimal pricePerPerson = new BigDecimal("250000.00");
        final BigDecimal expectedTotal = pricePerPerson.multiply(BigDecimal.valueOf(personsToBook));

        PackageTrip pkg = createTestPackage(initialCupos, pricePerPerson);
        final int packageId = pkg.getIdPaquete();

        // 1. Registro de nuevo cliente mediante /api/Usuarios/registro
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String clientEmail = "lifecycle." + uniqueSuffix + "@agencytrip.com";
        String clientPassword = "Password123*";

        String registerPayload = String.format(
                "{\"nombre\":\"Cliente\",\"apellido\":\"Prueba\",\"correo\":\"%s\",\"password\":\"%s\"}",
                clientEmail, clientPassword
        );

        MvcResult registerResult = mockMvc.perform(post("/api/Usuarios/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado correctamente."))
                .andExpect(jsonPath("$.usuario.correo").value(clientEmail))
                .andReturn();

        JsonNode regNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        JsonNode regUserNode = regNode.get("usuario");
        int createdUserId = regUserNode.has("idUsuario") ? regUserNode.get("idUsuario").asInt() : regUserNode.get("id").asInt();

        // 2. Autenticación y obtención de token JWT mediante /api/Usuarios/login
        String loginPayload = String.format(
                "{\"correo\":\"%s\",\"password\":\"%s\"}",
                clientEmail, clientPassword
        );

        MvcResult loginResult = mockMvc.perform(post("/api/Usuarios/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso."))
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = loginNode.get("token").asText();
        assertNotNull(jwtToken, "El token JWT no debe ser nulo");
        assertTrue(jwtToken.length() > 20, "El token JWT debe ser válido");

        // 3. Consulta de disponibilidad y cupos iniciales del paquete
        PackageTrip pkgCheck = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgCheck.getCupos(), "Los cupos iniciales deben coincidir con la configuración");

        // 4. Creación de una reserva en estado PENDIENTE (/api/Reservas)
        String reservationPayload = String.format(
                "{\"idPaquete\":%d,\"cantidadPersonas\":%d}",
                packageId, personsToBook
        );

        MvcResult reservationResult = mockMvc.perform(post("/api/Reservas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reservationPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Reserva creada correctamente."))
                .andExpect(jsonPath("$.reserva.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.reserva.cantidadPersonas").value(personsToBook))
                .andReturn();

        JsonNode resNode = objectMapper.readTree(reservationResult.getResponse().getContentAsString());
        int reservationId = resNode.get("reserva").get("idReserva").asInt();

        // 5. Verificación de regla de negocio: crear reserva PENDIENTE no descuenta cupos prematuramente
        PackageTrip pkgAfterPending = travelRepository.findPackage(packageId).orElseThrow();
        assertEquals(initialCupos, pkgAfterPending.getCupos(),
                "Los cupos del paquete no deben descontarse prematuramente mientras la reserva esté en PENDIENTE");

        // 6. Registro de pago con método PSE (/api/Pagos) asociado a la reserva
        String paymentPayload = String.format(
                "{\"idReserva\":%d,\"metodoPago\":\"PSE\"}",
                reservationId
        );

        MvcResult paymentResult = mockMvc.perform(post("/api/Pagos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Pago realizado correctamente."))
                .andExpect(jsonPath("$.pago.metodoPago").value("PSE"))
                .andExpect(jsonPath("$.pago.estado").value("APROBADO"))
                .andExpect(jsonPath("$.reserva.estado").value("CONFIRMADA"))
                .andReturn();

        JsonNode payNode = objectMapper.readTree(paymentResult.getResponse().getContentAsString());
        int paymentId = payNode.get("pago").get("idPago").asInt();

        // 7. Verificación del pago aprobado
        Payment dbPayment = travelRepository.findPayment(paymentId).orElseThrow();
        assertEquals("APROBADO", dbPayment.getEstado(), "El estado del pago debe ser APROBADO");
        assertEquals("PSE", dbPayment.getMetodoPago(), "El método de pago debe ser PSE");
        assertEquals(reservationId, dbPayment.getIdReserva(), "El pago debe estar asociado a la reserva correcta");
        assertEquals(0, expectedTotal.compareTo(dbPayment.getMonto()), "El monto del pago debe coincidir con el total de la reserva");

        // 8. Verificación de que la reserva pasó a estado CONFIRMADA
        Reservation dbReservation = travelRepository.findReservation(reservationId).orElseThrow();
        assertEquals("CONFIRMADA", dbReservation.getEstado(), "La reserva debe encontrarse en estado CONFIRMADA");
        assertEquals(personsToBook, dbReservation.getCantidadPersonas());

        // 9. Verificación de descuento exacto de cupos disponibles
        PackageTrip pkgAfterApproval = travelRepository.findPackage(packageId).orElseThrow();
        int expectedRemainingCupos = initialCupos - personsToBook;
        assertEquals(expectedRemainingCupos, pkgAfterApproval.getCupos(),
                "Los cupos disponibles del paquete deben haberse descontado exactamente por la cantidad de personas reservadas");

        // 10. Consulta de reservas y pagos propios por el cliente autenticado
        mockMvc.perform(get("/api/Reservas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idReserva == " + reservationId + ")].estado").value("CONFIRMADA"));

        mockMvc.perform(get("/api/Pagos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idPago == " + paymentId + ")].estado").value("APROBADO"));

        // Verificación de consistencia final en base de datos
        assertTrue(dbPayment.getFechaPago() != null, "La fecha de pago debe haber sido registrada");
        assertEquals(createdUserId, dbReservation.getIdUsuario(), "La reserva debe pertenecer al cliente registrado");
    }
}
