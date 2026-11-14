package com.hotel.sistema_hoteleria.Habitacion;

import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "habitaciones")
@Getter
@Setter
@NoArgsConstructor
public class Habitacion {

    public enum Estado {
        HABILITADA,
        MANTENIMIENTO,
        INACTIVA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El número de habitación es obligatorio")
    @Size(
            max = 10,
            message = "El número no puede superar 10 caracteres"
    )
    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @NotNull(message = "El piso es obligatorio")
    @Column(nullable = false)
    private Integer piso;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado;

    @NotNull(message = "El tipo de habitación es obligatorio")
    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_habitacion_id", nullable = false)
    private TipoHabitacion tipoHabitacion;
}