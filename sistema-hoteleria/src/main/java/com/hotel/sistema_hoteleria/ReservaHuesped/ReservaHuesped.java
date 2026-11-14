package com.hotel.sistema_hoteleria.ReservaHuesped;

import com.hotel.sistema_hoteleria.Huesped.Huesped;
import com.hotel.sistema_hoteleria.Reserva.Reserva;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "reservas_huespedes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_reserva_huesped",
                        columnNames = {
                                "reserva_id",
                                "huesped_id"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReservaHuesped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La reserva es obligatoria")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "reserva_id",
            nullable = false
    )
    private Reserva reserva;

    @NotNull(message = "El huésped es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(
            name = "huesped_id",
            nullable = false
    )
    private Huesped huesped;

    @NotNull(message = "La fecha de entrada es obligatoria")
    @Column(name = "fecha_entrada", nullable = false)
    private LocalDate fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    @Column(name = "fecha_salida", nullable = false)
    private LocalDate fechaSalida;
}