package com.agencytrip.agencytripapi.domain.model;
import java.math.BigDecimal; import java.time.LocalDateTime;
public class Payment {
    private Integer idPago; private Integer idReserva; private LocalDateTime fechaPago; private BigDecimal monto; private String metodoPago; private String estado; private Reservation reserva;
    public Integer getIdPago(){return idPago;} public void setIdPago(Integer v){idPago=v;} public Integer getIdReserva(){return idReserva;} public void setIdReserva(Integer v){idReserva=v;} public LocalDateTime getFechaPago(){return fechaPago;} public void setFechaPago(LocalDateTime v){fechaPago=v;} public BigDecimal getMonto(){return monto;} public void setMonto(BigDecimal v){monto=v;} public String getMetodoPago(){return metodoPago;} public void setMetodoPago(String v){metodoPago=v;} public String getEstado(){return estado;} public void setEstado(String v){estado=v;} public Reservation getReserva(){return reserva;} public void setReserva(Reservation v){reserva=v;}
}
