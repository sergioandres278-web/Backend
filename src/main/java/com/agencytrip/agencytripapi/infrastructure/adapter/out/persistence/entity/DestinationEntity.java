package com.agencytrip.agencytripapi.infrastructure.adapter.out.persistence.entity;
import jakarta.persistence.*; import lombok.*;
@Entity @Table(name="destinos") @Getter @Setter @NoArgsConstructor public class DestinationEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_destino") private Integer idDestino; private String nombre; private String pais; private String descripcion; private String imagen; private boolean estado; }
