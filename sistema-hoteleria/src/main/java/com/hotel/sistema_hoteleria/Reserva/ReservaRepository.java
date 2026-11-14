package com.hotel.sistema_hoteleria.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByHabitacionId(Long habitacionId);

    List<Reserva> findByEstado(String estado);

    List<Reserva> findByFechaEntradaBetween(
            LocalDate fechaInicio,
            LocalDate fechaFin
    );

    boolean existsByHabitacionIdAndFechaEntradaLessThanEqualAndFechaSalidaGreaterThanEqual(
            Long habitacionId,
            LocalDate fechaSalidaNueva,
            LocalDate fechaEntradaNueva
    );
}