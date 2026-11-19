package com.hotel.sistema_hoteleria.Reserva;

import java.math.BigDecimal;

public record HabitacionDisponible(
        Long id,
        String numero,
        Integer piso,
        String tipo,
        Integer capacidad,
        BigDecimal precioPorNoche,
        long noches,
        BigDecimal total
) {
}