package com.banco.digital.config;

import com.banco.digital.models.VerifiedIdentity;
import com.banco.digital.repositories.VerifiedIdentityRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(VerifiedIdentityRepository repository) {
        return args -> {
            // Limpiar datos anteriores para asegurar que se carguen los nuevos cambios
            repository.deleteAll();

            repository.saveAll(List.of(
                    VerifiedIdentity.builder()
                            .documentNumber("12345678")
                            .fullName("Juan Perez")
                            .expeditionDate("2015-05-20")
                            .expeditionPlace("Bogotá")
                            .build(),
                    VerifiedIdentity.builder()
                            .documentNumber("1035520443")
                            .fullName("Juan Andres Gonzalez Garcia")
                            .expeditionDate("2024-02-19")
                            .expeditionPlace("Guadalupe")
                            .build(),
                    VerifiedIdentity.builder()
                            .documentNumber("1035522346")
                            .fullName("Juan Daniel Gomez")
                            .expeditionDate("2019-01-22")
                            .expeditionPlace("Medellin")
                            .build(),
                    // New verification entry for María López
                    VerifiedIdentity.builder()
                            .documentNumber("98765432")
                            .fullName("María López")
                            .expeditionDate("2022-08-15")
                            .expeditionPlace("Cali")
                            .build(),
                    VerifiedIdentity.builder()
                            .documentNumber("11111111")
                            .fullName("Usuario Test")
                            .expeditionDate("2023-01-01")
                            .expeditionPlace("Test City")
                            .build(),
                    VerifiedIdentity.builder()
                            .documentNumber("22222222")
                            .fullName("Usuario Test 2")
                            .expeditionDate("2023-01-02")
                            .expeditionPlace("Test City 2")
                            .build(),
                    VerifiedIdentity.builder()
                                .documentNumber("33333333")
                                .fullName("Test 3")
                                .expeditionDate("2023-01-03")
                                .expeditionPlace("Medellin")
                                .build()
                   
                ));
            System.out.println(">>> KYC Local: Identidades de prueba ACTUALIZADAS correctamente.");
        };
    }
}
