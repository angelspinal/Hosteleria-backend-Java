package com.hotel.sistema_hoteleria.TipoHabitacion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoHabitacionRepository
        extends JpaRepository<TipoHabitacion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(
            String nombre,
            Long id
    );
}