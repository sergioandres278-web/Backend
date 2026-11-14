package com.agencytrip.agencytripapi.domain.model;

import java.time.LocalDateTime;

public class SocialAccount {
    private Integer idCuentaSocial;
    private Integer idUsuario;
    private String proveedor;
    private String idProveedor;
    private String correoProveedor;
    private LocalDateTime fechaVinculacion;
    private User usuario;

    public Integer getIdCuentaSocial() { return idCuentaSocial; }
    public void setIdCuentaSocial(Integer idCuentaSocial) { this.idCuentaSocial = idCuentaSocial; }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public String getIdProveedor() { return idProveedor; }
    public void setIdProveedor(String idProveedor) { this.idProveedor = idProveedor; }

    public String getCorreoProveedor() { return correoProveedor; }
    public void setCorreoProveedor(String correoProveedor) { this.correoProveedor = correoProveedor; }

    public LocalDateTime getFechaVinculacion() { return fechaVinculacion; }
    public void setFechaVinculacion(LocalDateTime fechaVinculacion) { this.fechaVinculacion = fechaVinculacion; }

    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
}

