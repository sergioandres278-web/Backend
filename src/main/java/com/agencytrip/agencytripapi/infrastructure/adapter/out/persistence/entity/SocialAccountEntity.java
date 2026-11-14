package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "cuentas_sociales",
    uniqueConstraints = @UniqueConstraint(name = "uk_proveedor_id", columnNames = {"proveedor", "id_proveedor"})
)
@Getter
@Setter
@NoArgsConstructor
public class SocialAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta_social")
    private Integer idCuentaSocial;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "proveedor", length = 20, nullable = false)
    private String proveedor;

    @Column(name = "id_proveedor", length = 100, nullable = false)
    private String idProveedor;

    @Column(name = "correo_proveedor", length = 150, nullable = false)
    private String correoProveedor;

    @Column(name = "fecha_vinculacion")
    private LocalDateTime fechaVinculacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private UserEntity usuario;
}

