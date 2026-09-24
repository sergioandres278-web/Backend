package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="hoteles") @Getter @Setter @NoArgsConstructor public class HotelEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_hotel") private Integer idHotel; private String nombre; private String direccion; private String ciudad; private Integer categoria; private String descripcion; private boolean estado; }
