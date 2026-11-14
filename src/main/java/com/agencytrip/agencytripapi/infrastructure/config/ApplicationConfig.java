package com.agencytrip.agencytripapi.infrastructure.config;

import com.agencytrip.agencytripapi.application.port.out.*;
import com.agencytrip.agencytripapi.application.service.*;
import com.agencytrip.agencytripapi.domain.port.out.TravelRepositoryPort;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestClient;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    CatalogService catalogService(TravelRepositoryPort r) {
        return new CatalogService(r);
    }

    @Bean
    PackageService packageService(TravelRepositoryPort r) {
        return new PackageService(r);
    }

    @Bean
    UserService userService(TravelRepositoryPort r, PasswordHasherPort p, TokenPort t, GoogleTokenVerifierPort g, FacebookTokenVerifierPort f) {
        return new UserService(r, p, t, g, f);
    }

    @Bean
    ReservationService reservationService(TravelRepositoryPort r) {
        return new ReservationService(r);
    }

    @Bean
    PaymentService paymentService(TravelRepositoryPort r) {
        return new PaymentService(r);
    }
}

