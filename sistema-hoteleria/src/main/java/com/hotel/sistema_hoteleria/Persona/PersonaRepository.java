package com.hotel.sistema_hoteleria.Persona;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository
        extends JpaRepository<Persona, Long> {

    boolean existsByDocumentoIgnoreCase(String documento);

    boolean existsByDocumentoIgnoreCaseAndIdNot(
            String documento,
            Long id
    );
}