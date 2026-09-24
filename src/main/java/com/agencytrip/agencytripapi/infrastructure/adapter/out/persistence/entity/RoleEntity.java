package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="roles") @Getter @Setter @NoArgsConstructor public class RoleEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_rol") private Integer idRol; @Column(name="nombre") private String nombre; }
