package com.agencytrip.agencytripapi.infrastructure.config;
import com.agencytrip.agencytripapi.application.port.out.*; import com.agencytrip.agencytripapi.application.service.*; import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort; import org.springframework.context.annotation.*;
@Configuration public class ApplicationConfig {
 @Bean CatalogService catalogService(TravelRepositoryPort r){return new CatalogService(r);} @Bean PackageService packageService(TravelRepositoryPort r){return new PackageService(r);} @Bean UserService userService(TravelRepositoryPort r,PasswordHasherPort p,TokenPort t){return new UserService(r,p,t);} @Bean ReservationService reservationService(TravelRepositoryPort r){return new ReservationService(r);} @Bean PaymentService paymentService(TravelRepositoryPort r){return new PaymentService(r);}
}
