package com.hotel.sistema_hoteleria.Reserva;

import com.hotel.sistema_hoteleria.Habitacion.Habitacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reservas")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La habitación es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "habitacion_id",
            nullable = false
    )
    private Habitacion habitacion;

    @NotNull(message = "La fecha de entrada es obligatoria")
    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;

    @NotBlank(message = "El estado es obligatorio")
    @Column(nullable = false, length = 30)
    private String estado;

    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal total;
}