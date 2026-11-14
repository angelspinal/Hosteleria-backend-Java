package com.hotel.sistema_hoteleria.Habitacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitacionRepository
        extends JpaRepository<Habitacion, Long> {

    boolean existsByNumeroIgnoreCase(String numero);

    boolean existsByNumeroIgnoreCaseAndIdNot(
            String numero,
            Long id
    );
}