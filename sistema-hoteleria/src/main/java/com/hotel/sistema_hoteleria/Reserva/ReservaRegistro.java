package com.hotel.sistema_hoteleria.Reserva;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record ReservaRegistro(

        @NotNull(message = "La habitación es obligatoria")
        @Positive(message = "El id de habitación debe ser positivo")
        Long habitacionId,

        @NotNull(message = "La fecha de entrada es obligatoria")
        LocalDate fechaEntrada,

        @NotNull(message = "La fecha de salida es obligatoria")
        LocalDate fechaSalida,

        String estado,

        @NotEmpty(message = "Selecciona al menos un huésped")
        List<
                @NotNull(message = "El huésped es obligatorio")
                @Positive(message = "El id del huésped debe ser positivo")
                Long
                > huespedIds
) {
}