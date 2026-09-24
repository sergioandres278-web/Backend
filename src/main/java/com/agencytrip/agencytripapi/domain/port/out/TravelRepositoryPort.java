package com.agencytrip.agencytripapi.domain.port.out;

import com.agencytrip.agencytripapi.domain.model.*;
import java.util.*;

/** Puerto de salida: la aplicación no conoce JPA ni MySQL. */
public interface TravelRepositoryPort {
    Optional<User> findUserByEmail(String email); Optional<User> findUserById(int id); boolean emailExists(String email, Integer excludedId); List<User> findActiveUsers(); User saveUser(User user); Optional<Role> findRole(int id); List<Role> findRoles();
    List<Destination> findActiveDestinations(); Optional<Destination> findDestination(int id); Destination saveDestination(Destination destination);
    List<Hotel> findActiveHotels(); Optional<Hotel> findHotel(int id); Hotel saveHotel(Hotel hotel);
    List<Transport> findActiveTransports(); Optional<Transport> findTransport(int id); Transport saveTransport(Transport transport);
    List<PackageTrip> findActivePackages(); Optional<PackageTrip> findPackage(int id); PackageTrip savePackage(PackageTrip trip);
    List<Reservation> findReservations(); List<Reservation> findReservationsByUser(int userId); Optional<Reservation> findReservation(int id); Reservation saveReservation(Reservation reservation);
    List<Payment> findPayments(); List<Payment> findPaymentsByUser(int userId); Optional<Payment> findPayment(int id); boolean hasApprovedPayment(int reservationId); Payment savePayment(Payment payment);
}
