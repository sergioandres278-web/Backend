package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="transportes") @Getter @Setter @NoArgsConstructor public class TransportEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_transporte") private Integer idTransporte; private String tipo; private String empresa; private String descripcion; private boolean estado; }
