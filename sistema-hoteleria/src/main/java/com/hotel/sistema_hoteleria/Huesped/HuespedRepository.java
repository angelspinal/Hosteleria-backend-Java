package com.hotel.sistema_hoteleria.Huesped;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HuespedRepository
        extends JpaRepository<Huesped, Long> {

    boolean existsByPersona_Id(Long personaId);

    boolean existsByPersona_IdAndIdNot(
            Long personaId,
            Long id
    );
}