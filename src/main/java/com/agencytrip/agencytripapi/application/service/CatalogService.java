package com.agencytrip.agencytripapi.application.service;

import com.agencytrip.agencytripapi.application.dto.Requests;
import com.agencytrip.agencytripapi.domain.model.*;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;
import java.util.*; import org.springframework.http.HttpStatus;

/** Casos de uso de catálogos; no depende de HTTP, JPA ni MySQL. */
public class CatalogService {
    private final TravelRepositoryPort repo;
    public CatalogService(TravelRepositoryPort repo) { this.repo=repo; }
    public List<Destination> destinations(){ return repo.findActiveDestinations(); }
    public Destination createDestination(Requests.Destination r){ Destination d=new Destination(); d.setNombre(r.nombre()); d.setPais(r.pais()); d.setDescripcion(r.descripcion()); d.setImagen(r.imagen()); d.setEstado(true); return repo.saveDestination(d); }
    public Destination updateDestination(int id, Requests.Destination r){ Destination d=repo.findDestination(id).orElseThrow(()->notFound("El destino no existe.")); d.setNombre(r.nombre());d.setPais(r.pais());d.setDescripcion(r.descripcion());d.setImagen(r.imagen());return repo.saveDestination(d); }
    public void disableDestination(int id){ Destination d=repo.findDestination(id).orElseThrow(()->notFound("El destino no existe.")); d.setEstado(false);repo.saveDestination(d); }
    public List<Hotel> hotels(){return repo.findActiveHotels();}
    public Hotel createHotel(Requests.Hotel r){ validateCategory(r.categoria()); Hotel h=new Hotel(); apply(h,r);h.setEstado(true);return repo.saveHotel(h); }
    public Hotel updateHotel(int id, Requests.Hotel r){ Hotel h=repo.findHotel(id).orElseThrow(()->notFound("El hotel no existe."));if(!h.isEstado()) throw bad("El hotel está desactivado.");validateCategory(r.categoria());apply(h,r);return repo.saveHotel(h); }
    public void disableHotel(int id){Hotel h=repo.findHotel(id).orElseThrow(()->notFound("El hotel no existe."));if(!h.isEstado())throw bad("El hotel ya está desactivado.");h.setEstado(false);repo.saveHotel(h);}
    public List<Transport> transports(){return repo.findActiveTransports();}
    public Transport createTransport(Requests.Transport r){Transport t=new Transport();apply(t,r);t.setEstado(true);return repo.saveTransport(t);}
    public Transport updateTransport(int id,Requests.Transport r){Transport t=repo.findTransport(id).orElseThrow(()->notFound("El transporte no existe."));if(!t.isEstado())throw bad("El transporte está desactivado.");apply(t,r);return repo.saveTransport(t);}
    public void disableTransport(int id){Transport t=repo.findTransport(id).orElseThrow(()->notFound("El transporte no existe."));if(!t.isEstado())throw bad("El transporte ya está desactivado.");t.setEstado(false);repo.saveTransport(t);}
    private void validateCategory(Integer c){if(c!=null&&(c<1||c>5))throw bad("La categoría debe estar entre 1 y 5.");}
    private void apply(Hotel h,Requests.Hotel r){h.setNombre(r.nombre());h.setDireccion(r.direccion());h.setCiudad(r.ciudad());h.setCategoria(r.categoria());h.setDescripcion(r.descripcion());}
    private void apply(Transport t,Requests.Transport r){t.setTipo(r.tipo());t.setEmpresa(r.empresa());t.setDescripcion(r.descripcion());}
    static BusinessException bad(String m){return new BusinessException(HttpStatus.BAD_REQUEST,m);} static BusinessException notFound(String m){return new BusinessException(HttpStatus.NOT_FOUND,m);}
}
