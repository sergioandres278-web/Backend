package com.agencytrip.agencytripapi.domain.model;

import java.time.LocalDateTime;
public class User {
    private Integer idUsuario; private String nombre; private String apellido; private String correo; private String password;
    private Integer idRol; private LocalDateTime fechaRegistro; private boolean estado; private Role rol;
    public Integer getIdUsuario() { return idUsuario; } public void setIdUsuario(Integer v) { idUsuario=v; }
    public String getNombre() { return nombre; } public void setNombre(String v) { nombre=v; }
    public String getApellido() { return apellido; } public void setApellido(String v) { apellido=v; }
    public String getCorreo() { return correo; } public void setCorreo(String v) { correo=v; }
    public String getPassword() { return password; } public void setPassword(String v) { password=v; }
    public Integer getIdRol() { return idRol; } public void setIdRol(Integer v) { idRol=v; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; } public void setFechaRegistro(LocalDateTime v) { fechaRegistro=v; }
    public boolean isEstado() { return estado; } public void setEstado(boolean v) { estado=v; }
    public Role getRol() { return rol; } public void setRol(Role v) { rol=v; }
}
