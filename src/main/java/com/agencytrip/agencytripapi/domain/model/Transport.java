package com.agencytrip.agencytripapi.domain.model;
public class Transport {
    private Integer idTransporte; private String tipo; private String empresa; private String descripcion; private boolean estado;
    public Integer getIdTransporte(){return idTransporte;} public void setIdTransporte(Integer v){idTransporte=v;} public String getTipo(){return tipo;} public void setTipo(String v){tipo=v;} public String getEmpresa(){return empresa;} public void setEmpresa(String v){empresa=v;} public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;} public boolean isEstado(){return estado;} public void setEstado(boolean v){estado=v;}
}
