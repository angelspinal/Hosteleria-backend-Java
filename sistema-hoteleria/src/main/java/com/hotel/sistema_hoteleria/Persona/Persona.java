package com.hotel.sistema_hoteleria.Persona;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    @Column(nullable = false, length = 80)
    private String apellido;

    @NotBlank(message = "El documento es obligatorio")
    @Size(max = 30, message = "El documento no puede superar 30 caracteres")
    @Column(nullable = false, unique = true, length = 30)
    private String documento;

    @NotBlank(message = "El teléfono es obligatorio")
    @Size(max = 25, message = "El teléfono no puede superar 25 caracteres")
    @Column(nullable = false, length = 25)
    private String telefono;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    @Column(nullable = false, length = 150)
    private String correo;
}