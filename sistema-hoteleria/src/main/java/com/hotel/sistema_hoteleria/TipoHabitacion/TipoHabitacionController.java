package com.hotel.sistema_hoteleria.TipoHabitacion;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-habitacion")
@RequiredArgsConstructor
public class TipoHabitacionController {

    private final TipoHabitacionRepository repo;

    @GetMapping("/listar")
    public List<TipoHabitacion> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<TipoHabitacion> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody TipoHabitacion datos
    ) {
        String nombre = datos.getNombre().trim();

        if (repo.existsByNombreIgnoreCase(nombre)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe un tipo de habitación con el nombre: "
                                    + nombre
                    );
        }

        datos.setId(null);
        datos.setNombre(nombre);

        TipoHabitacion guardado = repo.save(datos);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/tipos-habitacion/obtener/"
                                        + guardado.getId()
                        )
                )
                .body(guardado);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody TipoHabitacion datos
    ) {
        TipoHabitacion existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        String nombre = datos.getNombre().trim();

        if (repo.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe otro tipo de habitación con el nombre: "
                                    + nombre
                    );
        }

        existente.setNombre(nombre);
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrecioBase(datos.getPrecioBase());
        existente.setCapacidad(datos.getCapacidad());

        return ResponseEntity.ok(repo.save(existente));
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable("id") Long id
    ) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repo.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}