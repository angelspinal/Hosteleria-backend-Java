package com.hotel.sistema_hoteleria.Huesped;

import com.hotel.sistema_hoteleria.Persona.Persona;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "huespedes")
@Getter
@Setter
@NoArgsConstructor
public class Huesped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La persona es obligatoria")
    @OneToOne(optional = false)
    @JoinColumn(
            name = "persona_id",
            nullable = false,
            unique = true
    )
    private Persona persona;
}