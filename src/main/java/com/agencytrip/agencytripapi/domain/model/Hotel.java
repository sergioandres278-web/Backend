package com.agencytrip.agencytripapi.domain.model;
public class Hotel {
    private Integer idHotel; private String nombre; private String direccion; private String ciudad; private Integer categoria; private String descripcion; private boolean estado;
    public Integer getIdHotel(){return idHotel;} public void setIdHotel(Integer v){idHotel=v;} public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;} public String getDireccion(){return direccion;} public void setDireccion(String v){direccion=v;} public String getCiudad(){return ciudad;} public void setCiudad(String v){ciudad=v;} public Integer getCategoria(){return categoria;} public void setCategoria(Integer v){categoria=v;} public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public boolean isEstado(){return estado;} public void setEstado(boolean v){estado=v;}
}
