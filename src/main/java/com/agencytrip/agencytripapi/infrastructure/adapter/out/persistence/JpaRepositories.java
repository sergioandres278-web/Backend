package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.DestinationEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.HotelEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.PackageEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.PaymentEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.ReservationEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.RoleEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.TransportEntity;
import com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity.UserEntity;

interface UserJpaRepository extends JpaRepository<UserEntity, Integer> {

    @EntityGraph(attributePaths = {"rol"})
    Optional<UserEntity> findByCorreo(String correo);

    @EntityGraph(attributePaths = {"rol"})
    Optional<UserEntity> findById(Integer id);

    @EntityGraph(attributePaths = {"rol"})
    List<UserEntity> findByEstadoTrue();

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdUsuarioNot(String correo, Integer id);
}

interface RoleJpaRepository extends JpaRepository<RoleEntity, Integer> {
}

interface DestinationJpaRepository extends JpaRepository<DestinationEntity, Integer> {

    List<DestinationEntity> findByEstadoTrue();
}

interface HotelJpaRepository extends JpaRepository<HotelEntity, Integer> {

    List<HotelEntity> findByEstadoTrue();
}

interface TransportJpaRepository extends JpaRepository<TransportEntity, Integer> {

    List<TransportEntity> findByEstadoTrue();
}

interface PackageJpaRepository extends JpaRepository<PackageEntity, Integer> {

    @EntityGraph(attributePaths = {"destino", "transporte", "hotel"})
    List<PackageEntity> findByEstadoTrue();

    @EntityGraph(attributePaths = {"destino", "transporte", "hotel"})
    Optional<PackageEntity> findById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PackageEntity p WHERE p.idPaquete = :id")
    Optional<PackageEntity> findByIdForUpdate(@Param("id") Integer id);
}

interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Integer> {

    @EntityGraph(attributePaths = {
        "usuario",
        "usuario.rol",
        "paquete",
        "paquete.destino",
        "paquete.transporte",
        "paquete.hotel"
    })
    List<ReservationEntity> findAll();

    @EntityGraph(attributePaths = {
        "usuario",
        "usuario.rol",
        "paquete",
        "paquete.destino",
        "paquete.transporte",
        "paquete.hotel"
    })
    List<ReservationEntity> findByIdUsuario(Integer idUsuario);

    @EntityGraph(attributePaths = {
        "usuario",
        "usuario.rol",
        "paquete",
        "paquete.destino",
        "paquete.transporte",
        "paquete.hotel"
    })
    Optional<ReservationEntity> findById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservationEntity r WHERE r.idReserva = :id")
    Optional<ReservationEntity> findByIdForUpdate(@Param("id") Integer id);
}

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Integer> {

    @EntityGraph(attributePaths = {
        "reserva",
        "reserva.usuario",
        "reserva.usuario.rol",
        "reserva.paquete",
        "reserva.paquete.destino",
        "reserva.paquete.transporte",
        "reserva.paquete.hotel"
    })
    List<PaymentEntity> findAll();

    @EntityGraph(attributePaths = {
        "reserva",
        "reserva.usuario",
        "reserva.usuario.rol",
        "reserva.paquete",
        "reserva.paquete.destino",
        "reserva.paquete.transporte",
        "reserva.paquete.hotel"
    })
    List<PaymentEntity> findByReservaIdUsuario(Integer idUsuario);

    @EntityGraph(attributePaths = {
        "reserva",
        "reserva.usuario",
        "reserva.usuario.rol",
        "reserva.paquete",
        "reserva.paquete.destino",
        "reserva.paquete.transporte",
        "reserva.paquete.hotel"
    })
    Optional<PaymentEntity> findById(Integer id);

    boolean existsByIdReservaAndEstado(Integer idReserva, String estado);
}