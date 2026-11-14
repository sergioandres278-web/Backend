package com.agencytrip.agencytripapi.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.Destination;
import com.agencytrip.agencytripapi.domain.model.Hotel;
import com.agencytrip.agencytripapi.domain.model.PackageTrip;
import com.agencytrip.agencytripapi.domain.model.Payment;
import com.agencytrip.agencytripapi.domain.model.Reservation;
import com.agencytrip.agencytripapi.domain.model.Role;
import com.agencytrip.agencytripapi.domain.model.Transport;
import com.agencytrip.agencytripapi.domain.model.User;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;

class PaymentConcurrencyTests {

    @Test
    void preventsDuplicatePaymentsOnConcurrentRequests() throws Exception {
        int reservationId = 100;
        int userId = 10;
        int packageId = 20;

        PackageTrip pkg = new PackageTrip();
        pkg.setIdPaquete(packageId);
        pkg.setCupos(10);
        pkg.setEstado(true);

        Reservation res = new Reservation();
        res.setIdReserva(reservationId);
        res.setIdUsuario(userId);
        res.setIdPaquete(packageId);
        res.setCantidadPersonas(2);
        res.setPrecioTotal(new BigDecimal("100000"));
        res.setEstado("PENDIENTE");

        List<Payment> savedPayments = Collections.synchronizedList(new ArrayList<>());
        ReentrantLock reservationLock = new ReentrantLock();

        TravelRepositoryPort syncRepo = new DummySyncRepository() {
            @Override
            public Optional<Reservation> findReservationForUpdate(int id) {
                if (id == reservationId) {
                    reservationLock.lock();
                    Reservation copy = new Reservation();
                    copy.setIdReserva(res.getIdReserva());
                    copy.setIdUsuario(res.getIdUsuario());
                    copy.setIdPaquete(res.getIdPaquete());
                    copy.setCantidadPersonas(res.getCantidadPersonas());
                    copy.setPrecioTotal(res.getPrecioTotal());
                    copy.setEstado(res.getEstado());
                    return Optional.of(copy);
                }
                return Optional.empty();
            }

            @Override
            public Optional<PackageTrip> findPackageForUpdate(int id) {
                if (id == packageId) {
                    PackageTrip copy = new PackageTrip();
                    copy.setIdPaquete(pkg.getIdPaquete());
                    copy.setCupos(pkg.getCupos());
                    copy.setEstado(pkg.isEstado());
                    return Optional.of(copy);
                }
                return Optional.empty();
            }

            @Override
            public boolean hasApprovedPayment(int rId) {
                return savedPayments.stream().anyMatch(p -> p.getIdReserva() == rId && "APROBADO".equals(p.getEstado()));
            }

            @Override
            public Reservation saveReservation(Reservation r) {
                res.setEstado(r.getEstado());
                return r;
            }

            @Override
            public PackageTrip savePackage(PackageTrip p) {
                pkg.setCupos(p.getCupos());
                return p;
            }

            @Override
            public Payment savePayment(Payment p) {
                savedPayments.add(p);
                if (reservationLock.isHeldByCurrentThread()) {
                    reservationLock.unlock();
                }
                return p;
            }
        };

        PaymentService service = new PaymentService(syncRepo);

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    service.create(userId, new Requests.Payment(reservationId, "PSE"));
                    successCount.incrementAndGet();
                } catch (BusinessException be) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (reservationLock.isHeldByCurrentThread()) {
                        reservationLock.unlock();
                    }
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Solo una solicitud concurrente de pago debe tener éxito.");
        assertEquals(threads - 1, failureCount.get(), "Las solicitudes concurrentes duplicadas deben ser rechazadas.");
        assertEquals(1, savedPayments.size(), "No deben crearse pagos duplicados en el repositorio.");
        assertEquals("CONFIRMADA", res.getEstado(), "La reserva debe quedar confirmada.");
        assertEquals(8, pkg.getCupos(), "Los cupos solo deben descontarse una única vez.");
    }

    @Test
    void preventsOverbookingOnConcurrentPaymentsExceedingCapacity() throws Exception {
        int packageId = 50;
        int initialCupos = 2;

        PackageTrip pkg = new PackageTrip();
        pkg.setIdPaquete(packageId);
        pkg.setCupos(initialCupos);
        pkg.setEstado(true);

        // 3 reservas diferentes de 2 personas cada una para el mismo paquete (solo hay 2 cupos)
        Reservation r1 = new Reservation();
        r1.setIdReserva(1); r1.setIdUsuario(10); r1.setIdPaquete(packageId); r1.setCantidadPersonas(2); r1.setPrecioTotal(new BigDecimal("50000")); r1.setEstado("PENDIENTE");

        Reservation r2 = new Reservation();
        r2.setIdReserva(2); r2.setIdUsuario(11); r2.setIdPaquete(packageId); r2.setCantidadPersonas(2); r2.setPrecioTotal(new BigDecimal("50000")); r2.setEstado("PENDIENTE");

        Reservation r3 = new Reservation();
        r3.setIdReserva(3); r3.setIdUsuario(12); r3.setIdPaquete(packageId); r3.setCantidadPersonas(2); r3.setPrecioTotal(new BigDecimal("50000")); r3.setEstado("PENDIENTE");

        List<Reservation> reservations = List.of(r1, r2, r3);
        List<Payment> savedPayments = Collections.synchronizedList(new ArrayList<>());
        ReentrantLock packageLock = new ReentrantLock();

        TravelRepositoryPort syncRepo = new DummySyncRepository() {
            @Override
            public Optional<Reservation> findReservationForUpdate(int id) {
                return reservations.stream().filter(r -> r.getIdReserva() == id).findFirst();
            }

            @Override
            public Optional<PackageTrip> findPackageForUpdate(int id) {
                if (id == packageId) {
                    packageLock.lock();
                    PackageTrip copy = new PackageTrip();
                    copy.setIdPaquete(pkg.getIdPaquete());
                    copy.setCupos(pkg.getCupos());
                    copy.setEstado(pkg.isEstado());
                    return Optional.of(copy);
                }
                return Optional.empty();
            }

            @Override
            public boolean hasApprovedPayment(int rId) {
                return savedPayments.stream().anyMatch(p -> p.getIdReserva() == rId && "APROBADO".equals(p.getEstado()));
            }

            @Override
            public Reservation saveReservation(Reservation r) { return r; }

            @Override
            public PackageTrip savePackage(PackageTrip p) {
                pkg.setCupos(p.getCupos());
                return p;
            }

            @Override
            public Payment savePayment(Payment p) {
                savedPayments.add(p);
                if (packageLock.isHeldByCurrentThread()) {
                    packageLock.unlock();
                }
                return p;
            }
        };

        PaymentService service = new PaymentService(syncRepo);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                service.create(10, new Requests.Payment(1, "PSE"));
                successCount.incrementAndGet();
            } catch (BusinessException be) { failureCount.incrementAndGet(); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            finally {
                if (packageLock.isHeldByCurrentThread()) packageLock.unlock();
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                service.create(11, new Requests.Payment(2, "PSE"));
                successCount.incrementAndGet();
            } catch (BusinessException be) { failureCount.incrementAndGet(); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            finally {
                if (packageLock.isHeldByCurrentThread()) packageLock.unlock();
            }
        }));
        futures.add(executor.submit(() -> {
            try {
                startLatch.await();
                service.create(12, new Requests.Payment(3, "PSE"));
                successCount.incrementAndGet();
            } catch (BusinessException be) { failureCount.incrementAndGet(); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            finally {
                if (packageLock.isHeldByCurrentThread()) packageLock.unlock();
            }
        }));

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, successCount.get(), "Solo 1 reserva de 2 personas pudo confirmarse con los 2 cupos.");
        assertEquals(2, failureCount.get(), "Las otras 2 reservas debieron ser rechazadas por cupos insuficientes.");
        assertEquals(0, pkg.getCupos(), "Los cupos finales deben ser exactamente 0 (no negativos/sobreventa).");
        assertEquals(1, savedPayments.size(), "Solo se debió registrar 1 pago aprobado.");
    }

    private static class DummySyncRepository implements TravelRepositoryPort {
        @Override public Optional<User> findUserByEmail(String email) { return Optional.empty(); }
        @Override public Optional<User> findUserById(int id) { return Optional.empty(); }
        @Override public boolean emailExists(String email, Integer excludedId) { return false; }
        @Override public List<User> findActiveUsers() { return List.of(); }
        @Override public User saveUser(User user) { return user; }
        @Override public Optional<Role> findRole(int id) { return Optional.empty(); }
        @Override public List<Role> findRoles() { return List.of(); }
        @Override public List<Destination> findActiveDestinations() { return List.of(); }
        @Override public Optional<Destination> findDestination(int id) { return Optional.empty(); }
        @Override public Destination saveDestination(Destination destination) { return destination; }
        @Override public List<Hotel> findActiveHotels() { return List.of(); }
        @Override public Optional<Hotel> findHotel(int id) { return Optional.empty(); }
        @Override public Hotel saveHotel(Hotel hotel) { return hotel; }
        @Override public List<Transport> findActiveTransports() { return List.of(); }
        @Override public Optional<Transport> findTransport(int id) { return Optional.empty(); }
        @Override public Transport saveTransport(Transport transport) { return transport; }
        @Override public List<PackageTrip> findActivePackages() { return List.of(); }
        @Override public Optional<PackageTrip> findPackage(int id) { return Optional.empty(); }
        @Override public PackageTrip savePackage(PackageTrip trip) { return trip; }
        @Override public List<Reservation> findReservations() { return List.of(); }
        @Override public List<Reservation> findReservationsByUser(int userId) { return List.of(); }
        @Override public Optional<Reservation> findReservation(int id) { return Optional.empty(); }
        @Override public Reservation saveReservation(Reservation reservation) { return reservation; }
        @Override public List<Payment> findPayments() { return List.of(); }
        @Override public List<Payment> findPaymentsByUser(int userId) { return List.of(); }
        @Override public Optional<Payment> findPayment(int id) { return Optional.empty(); }
        @Override public boolean hasApprovedPayment(int reservationId) { return false; }
        @Override public Payment savePayment(Payment payment) { return payment; }
    }
}
