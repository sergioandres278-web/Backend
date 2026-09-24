package com.agencytrip.agencytripapi.domain.model;
public class Destination {
    private Integer idDestino; private String nombre; private String pais; private String descripcion; private String imagen; private boolean estado;
    public Integer getIdDestino(){return idDestino;} public void setIdDestino(Integer v){idDestino=v;} public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;} public String getPais(){return pais;} public void setPais(String v){pais=v;} public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public String getImagen(){return imagen;} public void setImagen(String v){imagen=v;} public boolean isEstado(){return estado;} public void setEstado(boolean v){estado=v;}
}
