package com.hotel.sistema_hoteleria.Habitacion;

import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacion;
import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacionRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {

    private final HabitacionRepository repo;
    private final TipoHabitacionRepository tipoRepo;

    @GetMapping("/listar")
    public List<Habitacion> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<Habitacion> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody Habitacion habitacion
    ) {
        String numero = habitacion.getNumero().trim();

        if (repo.existsByNumeroIgnoreCase(numero)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe una habitación con el número: "
                                    + numero
                    );
        }

        if (habitacion.getTipoHabitacion() == null
                || habitacion.getTipoHabitacion().getId() == null) {

            return ResponseEntity.badRequest()
                    .body("Debes indicar el id del tipo de habitación");
        }

        Long tipoId = habitacion.getTipoHabitacion().getId();

        TipoHabitacion tipo = tipoRepo.findById(tipoId)
                .orElse(null);

        if (tipo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            "No existe un tipo de habitación con id: "
                                    + tipoId
                    );
        }

        habitacion.setId(null);
        habitacion.setNumero(numero);
        habitacion.setTipoHabitacion(tipo);

        Habitacion guardada = repo.save(habitacion);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/habitaciones/obtener/"
                                        + guardada.getId()
                        )
                )
                .body(guardada);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Habitacion datos
    ) {
        Habitacion existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        String numero = datos.getNumero().trim();

        if (repo.existsByNumeroIgnoreCaseAndIdNot(numero, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe otra habitación con el número: "
                                    + numero
                    );
        }

        if (datos.getTipoHabitacion() == null
                || datos.getTipoHabitacion().getId() == null) {

            return ResponseEntity.badRequest()
                    .body("Debes indicar el id del tipo de habitación");
        }

        Long tipoId = datos.getTipoHabitacion().getId();

        TipoHabitacion tipo = tipoRepo.findById(tipoId)
                .orElse(null);

        if (tipo == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            "No existe un tipo de habitación con id: "
                                    + tipoId
                    );
        }

        existente.setNumero(numero);
        existente.setPiso(datos.getPiso());
        existente.setEstado(datos.getEstado());
        existente.setTipoHabitacion(tipo);

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