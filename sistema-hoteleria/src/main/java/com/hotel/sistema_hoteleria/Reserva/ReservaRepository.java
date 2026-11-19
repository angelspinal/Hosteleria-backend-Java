package com.hotel.sistema_hoteleria.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservaRepository
        extends JpaRepository<Reserva, Long> {

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

    @Query("""
            SELECT COUNT(r)
            FROM Reserva r
            WHERE r.habitacion.id = :habitacionId
              AND UPPER(TRIM(r.estado)) <> 'CANCELADA'
              AND r.fechaEntrada < :salida
              AND r.fechaSalida > :entrada
              AND (:excluirId IS NULL OR r.id <> :excluirId)
            """)
    long contarCruces(
            @Param("habitacionId") Long habitacionId,
            @Param("entrada") LocalDate entrada,
            @Param("salida") LocalDate salida,
            @Param("excluirId") Long excluirId
    );
}