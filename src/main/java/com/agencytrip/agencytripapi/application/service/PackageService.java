package com.agencytrip.agencytripapi.application.service;
import com.agencytrip.agencytripapi.application.dto.Requests; import com.agencytrip.agencytripapi.domain.model.PackageTrip; import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort; import java.util.*;
public class PackageService {
 private final TravelRepositoryPort repo; public PackageService(TravelRepositoryPort repo){this.repo=repo;}
 public List<PackageTrip> list(){return repo.findActivePackages();}
 public PackageTrip create(Requests.PackageTrip r){validate(r);PackageTrip p=new PackageTrip();apply(p,r);p.setEstado(true);return repo.savePackage(p);}
 public PackageTrip update(int id,Requests.PackageTrip r){PackageTrip p=repo.findPackage(id).orElseThrow(()->CatalogService.notFound("El paquete no existe."));if(!p.isEstado())throw CatalogService.bad("El paquete está desactivado.");validate(r);apply(p,r);return repo.savePackage(p);}
 public void disable(int id){PackageTrip p=repo.findPackage(id).orElseThrow(()->CatalogService.notFound("El paquete no existe."));if(!p.isEstado())throw CatalogService.bad("El paquete ya está desactivado.");p.setEstado(false);repo.savePackage(p);}
 private void validate(Requests.PackageTrip r){if(r.fechaRegreso().isBefore(r.fechaSalida()))throw CatalogService.bad("La fecha de regreso no puede ser anterior a la fecha de salida."); if(repo.findDestination(r.idDestino()).filter(x->x.isEstado()).isEmpty())throw CatalogService.bad("El destino no existe o no está disponible.");if(r.idTransporte()!=null&&repo.findTransport(r.idTransporte()).filter(x->x.isEstado()).isEmpty())throw CatalogService.bad("El transporte no existe o no está disponible.");if(r.idHotel()!=null&&repo.findHotel(r.idHotel()).filter(x->x.isEstado()).isEmpty())throw CatalogService.bad("El hotel no existe o no está disponible.");}
 private void apply(PackageTrip p,Requests.PackageTrip r){p.setNombre(r.nombre());p.setDescripcion(r.descripcion());p.setPrecio(r.precio());p.setFechaSalida(r.fechaSalida());p.setFechaRegreso(r.fechaRegreso());p.setCupos(r.cupos());p.setIdDestino(r.idDestino());p.setIdTransporte(r.idTransporte());p.setIdHotel(r.idHotel());}
}
