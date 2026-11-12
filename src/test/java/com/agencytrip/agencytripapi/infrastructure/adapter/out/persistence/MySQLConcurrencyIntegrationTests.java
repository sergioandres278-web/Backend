package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.AfterAll;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.application.service.BusinessException;
import com.agencytrip.agencytripapi.application.service.PaymentService;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.DestinationEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.HotelEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.PackageEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.ReservationEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.TransportEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.UserEntity;

/**
 * Pruebas de integraciÃ³n concurrentes contra MySQL real.
 * 
 * Valida:
 * 1. Bloqueo pesimista (SELECT ... FOR UPDATE) para evitar pagos duplicados ante solicitudes concurrentes.
 * 2. Bloqueo pesimista para evitar sobreventa de cupos ante confirmaciones de pago concurrentes.
 * 
 * Regla de integridad:
 * Solo crea y elimina registros transitorios aislados (con prefijo TEST_CONCURRENCY_),
 * garantizando que los datos existentes en la base de datos MySQL no se eliminen ni alteren.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MySQLConcurrencyIntegrationTests {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PackageJpaRepository packageRepo;

    @Autowired
    private ReservationJpaRepository reservationRepo;

    @Autowired
    private PaymentJpaRepository paymentRepo;

    @Autowired
    private UserJpaRepository userRepo;

    @Autowired
    private DestinationJpaRepository destinationRepo;

    @Autowired
    private HotelJpaRepository hotelRepo;

    @Autowired
    private TransportJpaRepository transportRepo;

    private final List<Integer> createdPaymentIds = new CopyOnWriteArrayList<>();
    private final List<Integer> createdReservationIds = new CopyOnWriteArrayList<>();
    private final List<Integer> createdPackageIds = new CopyOnWriteArrayList<>();

    private final String testRunId = UUID.randomUUID().toString();
private UserEntity testUser;
    private DestinationEntity testDestination;
    private HotelEntity testHotel;
    private TransportEntity testTransport;

    @BeforeEach
    void setUp() {
        testUser = userRepo.findByEstadoTrue().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Se requiere al menos un usuario activo en MySQL"));
        testDestination = destinationRepo.findByEstadoTrue().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Se requiere al menos un destino activo en MySQL"));
        testHotel = hotelRepo.findByEstadoTrue().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Se requiere al menos un hotel activo en MySQL"));
        testTransport = transportRepo.findByEstadoTrue().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Se requiere al menos un transporte activo en MySQL"));
    }

    @AfterEach
    void cleanUp() {
        // Limpieza quirÃºrgica: solo se borran los registros creados durante la prueba
        for (Integer paymentId : createdPaymentIds) {
            paymentRepo.findById(paymentId).ifPresent(paymentRepo::delete);
        }
        for (Integer resId : createdReservationIds) {
            // Eliminar pagos asociados por si se crearon durante la prueba
            List<PaymentEntity> associatedPayments = paymentRepo.findAll().stream()
                .filter(p -> p.getIdReserva() != null && p.getIdReserva().equals(resId))
                .toList();
            paymentRepo.deleteAll(associatedPayments);
            reservationRepo.findById(resId).ifPresent(reservationRepo::delete);
        }
        for (Integer pkgId : createdPackageIds) {
            packageRepo.findById(pkgId).ifPresent(packageRepo::delete);
        }
        createdPaymentIds.clear();
        createdReservationIds.clear();
        createdPackageIds.clear();
    }

    private PackageEntity createTestPackage(int cupos) {
        PackageEntity pkg = new PackageEntity();
        pkg.setNombre("TEST_CONCURRENCY_" + testRunId + "_" + UUID.randomUUID().toString().substring(0, 8));
        pkg.setDescripcion("Paquete de prueba para validaciÃ³n de concurrencia");
        pkg.setPrecio(new BigDecimal("150000.00"));
        pkg.setFechaSalida(LocalDateTime.now().plusDays(10));
        pkg.setFechaRegreso(LocalDateTime.now().plusDays(15));
        pkg.setCupos(cupos);
        pkg.setIdDestino(testDestination.getIdDestino());
        pkg.setIdHotel(testHotel.getIdHotel());
        pkg.setIdTransporte(testTransport.getIdTransporte());
        pkg.setEstado(true);

        PackageEntity saved = packageRepo.save(pkg);
        createdPackageIds.add(saved.getIdPaquete());
        return saved;
    }

    private ReservationEntity createTestReservation(int packageId, int personas) {
        ReservationEntity res = new ReservationEntity();
        res.setIdUsuario(testUser.getIdUsuario());
        res.setIdPaquete(packageId);
        res.setFechaReserva(LocalDateTime.now());
        res.setCantidadPersonas(personas);
        res.setPrecioTotal(new BigDecimal("150000.00").multiply(BigDecimal.valueOf(personas)));
        res.setEstado("PENDIENTE");

        ReservationEntity saved = reservationRepo.save(res);
        createdReservationIds.add(saved.getIdReserva());
        return saved;
    }

    @Test
    void testRealMySQLPreventsDuplicatePaymentsUnderConcurrency() throws Exception {
        PackageEntity pkg = createTestPackage(10);
        ReservationEntity res = createTestReservation(pkg.getIdPaquete(), 2);

        int reservationId = res.getIdReserva();
        int userId = testUser.getIdUsuario();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    PaymentService.PaymentResult result = paymentService.create(
                        userId,
                        new Requests.Payment(reservationId, "PSE")
                    );
                    if (result != null && result.payment() != null) {
                        createdPaymentIds.add(result.payment().getIdPago());
                        successCount.incrementAndGet();
                    }
                } catch (BusinessException be) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        // Disparar las solicitudes concurrentes al mismo milisegundo
        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // VerificaciÃ³n directa en MySQL
        List<PaymentEntity> actualPaymentsInDb = paymentRepo.findAll().stream()
            .filter(p -> p.getIdReserva() != null && p.getIdReserva().equals(reservationId))
            .toList();

        ReservationEntity updatedReservation = reservationRepo.findById(reservationId).orElseThrow();
        PackageEntity updatedPackage = packageRepo.findById(pkg.getIdPaquete()).orElseThrow();

        assertEquals(1, successCount.get(), "Exactamente una solicitud concurrente de pago debiÃ³ tener Ã©xito");
        assertEquals(threadCount - 1, failureCount.get(), "Las solicitudes concurrentes duplicadas debieron fallar");
        assertEquals(1, actualPaymentsInDb.size(), "MySQL no debe contener pagos duplicados para la misma reserva");
        assertEquals("APROBADO", actualPaymentsInDb.get(0).getEstado(), "El pago registrado en MySQL debe ser APROBADO");
        assertEquals("CONFIRMADA", updatedReservation.getEstado(), "La reserva en MySQL debe quedar CONFIRMADA");
        assertEquals(8, updatedPackage.getCupos(), "Los cupos del paquete en MySQL deben haberse descontado exactamente una vez (10 - 2 = 8)");
    }

    @Test
    void testRealMySQLPreventsOverbookingUnderConcurrentPayments() throws Exception {
        // Paquete con capacidad para exactamente 2 cupos
        PackageEntity pkg = createTestPackage(2);

        // Dos reservas independientes de 2 personas cada una
        ReservationEntity res1 = createTestReservation(pkg.getIdPaquete(), 2);
        ReservationEntity res2 = createTestReservation(pkg.getIdPaquete(), 2);

        int userId = testUser.getIdUsuario();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                PaymentService.PaymentResult r = paymentService.create(userId, new Requests.Payment(res1.getIdReserva(), "PSE"));
                createdPaymentIds.add(r.payment().getIdPago());
                successCount.incrementAndGet();
            } catch (BusinessException be) {
                failureCount.incrementAndGet();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }));

        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                PaymentService.PaymentResult r = paymentService.create(userId, new Requests.Payment(res2.getIdReserva(), "PSE"));
                createdPaymentIds.add(r.payment().getIdPago());
                successCount.incrementAndGet();
            } catch (BusinessException be) {
                failureCount.incrementAndGet();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }));

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // VerificaciÃ³n directa en MySQL
        PackageEntity updatedPackage = packageRepo.findById(pkg.getIdPaquete()).orElseThrow();
        ReservationEntity updatedRes1 = reservationRepo.findById(res1.getIdReserva()).orElseThrow();
        ReservationEntity updatedRes2 = reservationRepo.findById(res2.getIdReserva()).orElseThrow();

        assertEquals(1, successCount.get(), "Solo una de las reservas concurrentes debiÃ³ confirmarse con los 2 cupos disponibles");
        assertEquals(1, failureCount.get(), "La otra solicitud concurrente debiÃ³ fallar por cupos insuficientes");
        assertEquals(0, updatedPackage.getCupos(), "Los cupos del paquete en MySQL deben ser 0 (no sobreventa, no negativos)");

        boolean res1Confirmed = "CONFIRMADA".equals(updatedRes1.getEstado());
        boolean res2Confirmed = "CONFIRMADA".equals(updatedRes2.getEstado());
        assertTrue(res1Confirmed ^ res2Confirmed, "Exactamente una de las dos reservas debe estar CONFIRMADA y la otra PENDIENTE");
    }


    @AfterAll
    void cleanUpAll() {
        // Remove any leftover test packages and their related data.
        List<PackageEntity> testPackages = packageRepo.findAll().stream()
                .filter(p -> p.getNombre() != null && p.getNombre().contains(testRunId))
                .toList();
        for (PackageEntity pkg : testPackages) {
            // Remove associated reservations and payments
            List<ReservationEntity> resList = reservationRepo.findAll().stream()
                    .filter(r -> r.getIdPaquete() != null && r.getIdPaquete().equals(pkg.getIdPaquete()))
                    .toList();
            for (ReservationEntity res : resList) {
                // Delete payments linked to this reservation
                paymentRepo.findAll().stream()
                        .filter(p -> p.getIdReserva() != null && p.getIdReserva().equals(res.getIdReserva()))
                        .forEach(paymentRepo::delete);
                reservationRepo.delete(res);
            }
            packageRepo.delete(pkg);
        }
    }
}
