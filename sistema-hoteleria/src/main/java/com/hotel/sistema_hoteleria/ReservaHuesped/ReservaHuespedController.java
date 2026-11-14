package com.hotel.sistema_hoteleria.ReservaHuesped;

import com.hotel.sistema_hoteleria.Huesped.Huesped;
import com.hotel.sistema_hoteleria.Huesped.HuespedRepository;
import com.hotel.sistema_hoteleria.Reserva.Reserva;
import com.hotel.sistema_hoteleria.Reserva.ReservaRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/reservas-huespedes")
@RequiredArgsConstructor
public class ReservaHuespedController {

    private final ReservaHuespedRepository repo;
    private final ReservaRepository reservaRepo;
    private final HuespedRepository huespedRepo;

    @GetMapping("/listar")
    public List<ReservaHuesped> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<ReservaHuesped> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody ReservaHuesped datos
    ) {
        return guardar(null, datos);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReservaHuesped datos
    ) {
        ReservaHuesped existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        return guardar(existente, datos);
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

    private ResponseEntity<?> guardar(
            ReservaHuesped existente,
            ReservaHuesped datos
    ) {
        String error = validarDatos(datos);

        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Long reservaId = datos.getReserva().getId();
        Long huespedId = datos.getHuesped().getId();

        Reserva reserva = reservaRepo.findById(reservaId)
                .orElse(null);

        if (reserva == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe una reserva con id: " + reservaId);
        }

        Huesped huesped = huespedRepo.findById(huespedId)
                .orElse(null);

        if (huesped == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe un huésped con id: " + huespedId);
        }

        if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "No puedes registrar o modificar huéspedes "
                                    + "en una reserva cancelada"
                    );
        }

        if (datos.getFechaEntrada().isBefore(reserva.getFechaEntrada())
                || datos.getFechaSalida().isAfter(reserva.getFechaSalida())) {
            return ResponseEntity.badRequest()
                    .body(
                            "Las fechas del huésped deben estar "
                                    + "dentro del período de la reserva"
                    );
        }

        Long idExcluir = existente == null
                ? null
                : existente.getId();

        boolean duplicado = repo.findByReservaId(reservaId)
                .stream()
                .anyMatch(vinculacion ->
                        Objects.equals(
                                vinculacion.getHuesped().getId(),
                                huespedId
                        )
                                && !Objects.equals(
                                        vinculacion.getId(),
                                        idExcluir
                                )
                );

        if (duplicado) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Este huésped ya está asociado a la reserva");
        }

        boolean esNuevo = existente == null;

        ReservaHuesped vinculacion = esNuevo
                ? new ReservaHuesped()
                : existente;

        vinculacion.setReserva(reserva);
        vinculacion.setHuesped(huesped);
        vinculacion.setFechaEntrada(datos.getFechaEntrada());
        vinculacion.setFechaSalida(datos.getFechaSalida());

        ReservaHuesped guardado = repo.save(vinculacion);

        if (esNuevo) {
            return ResponseEntity
                    .created(
                            URI.create(
                                    "/api/reservas-huespedes/obtener/"
                                            + guardado.getId()
                            )
                    )
                    .body(guardado);
        }

        return ResponseEntity.ok(guardado);
    }

    private String validarDatos(ReservaHuesped datos) {
        if (datos.getReserva() == null
                || datos.getReserva().getId() == null) {
            return "Debes indicar el id de la reserva";
        }

        if (datos.getHuesped() == null
                || datos.getHuesped().getId() == null) {
            return "Debes indicar el id del huésped";
        }

        if (datos.getFechaEntrada() == null
                || datos.getFechaSalida() == null) {
            return "Las fechas de entrada y salida son obligatorias";
        }

        if (!datos.getFechaSalida().isAfter(datos.getFechaEntrada())) {
            return "La fecha de salida debe ser posterior a la entrada";
        }

        return null;
    }
}