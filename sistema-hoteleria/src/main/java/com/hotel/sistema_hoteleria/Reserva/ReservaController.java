package com.hotel.sistema_hoteleria.Reserva;

import com.hotel.sistema_hoteleria.Habitacion.Habitacion;
import com.hotel.sistema_hoteleria.Habitacion.HabitacionRepository;
import com.hotel.sistema_hoteleria.Pago.PagoRepository;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuesped;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuespedRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaRepository repo;
    private final HabitacionRepository habitacionRepo;
    private final ReservaHuespedRepository reservaHuespedRepo;
    private final PagoRepository pagoRepo;

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "CONFIRMADA",
            "CANCELADA",
            "FINALIZADA"
    );

    @GetMapping("/listar")
    public List<Reserva> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<Reserva> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody Reserva datos
    ) {
        String error = validarDatos(datos);

        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Long habitacionId = datos.getHabitacion().getId();

        Habitacion habitacion = habitacionRepo.findById(habitacionId)
                .orElse(null);

        if (habitacion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe una habitación con id: " + habitacionId);
        }

        String estado = normalizarEstado(datos.getEstado());

        if (!"CANCELADA".equals(estado)
                && existeCruce(
                        habitacionId,
                        datos.getFechaEntrada(),
                        datos.getFechaSalida(),
                        null
                )) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La habitación ya tiene una reserva en esas fechas");
        }

        Reserva nueva = new Reserva();
        nueva.setHabitacion(habitacion);
        nueva.setFechaEntrada(datos.getFechaEntrada());
        nueva.setFechaSalida(datos.getFechaSalida());
        nueva.setEstado(estado);
        nueva.setTotal(datos.getTotal());

        Reserva guardada = repo.save(nueva);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/reservas/obtener/" + guardada.getId()
                        )
                )
                .body(guardada);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Reserva datos
    ) {
        Reserva existente = repo.findById(id).orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        String error = validarDatos(datos);

        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Long habitacionId = datos.getHabitacion().getId();

        Habitacion habitacion = habitacionRepo.findById(habitacionId)
                .orElse(null);

        if (habitacion == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe una habitación con id: " + habitacionId);
        }

        String estado = normalizarEstado(datos.getEstado());

        if (!"CANCELADA".equals(estado)
                && existeCruce(
                        habitacionId,
                        datos.getFechaEntrada(),
                        datos.getFechaSalida(),
                        id
                )) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("La habitación ya tiene otra reserva en esas fechas");
        }

        List<ReservaHuesped> vinculaciones =
                reservaHuespedRepo.findByReservaId(id);

        for (ReservaHuesped vinculacion : vinculaciones) {
            LocalDate entrada = vinculacion.getFechaEntrada();
            LocalDate salida = vinculacion.getFechaSalida();

            if (entrada == null
                    || salida == null
                    || !salida.isAfter(entrada)
                    || entrada.isBefore(datos.getFechaEntrada())
                    || salida.isAfter(datos.getFechaSalida())) {

                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                "Las fechas de un huésped quedan fuera "
                                        + "del período de la reserva. "
                                        + "Revisa primero sus fechas"
                        );
            }
        }

        existente.setHabitacion(habitacion);
        existente.setFechaEntrada(datos.getFechaEntrada());
        existente.setFechaSalida(datos.getFechaSalida());
        existente.setEstado(estado);
        existente.setTotal(datos.getTotal());

        return ResponseEntity.ok(repo.save(existente));
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable("id") Long id
    ) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (!reservaHuespedRepo.findByReservaId(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "No puedes eliminar una reserva "
                                    + "que tiene huéspedes asociados"
                    );
        }

        if (!pagoRepo.findByReservaId(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "No puedes eliminar una reserva "
                                    + "que tiene pagos registrados"
                    );
        }

        repo.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private String validarDatos(Reserva datos) {
        if (datos.getHabitacion() == null
                || datos.getHabitacion().getId() == null) {
            return "Debes indicar el id de la habitación";
        }

        if (datos.getFechaEntrada() == null
                || datos.getFechaSalida() == null) {
            return "Las fechas de entrada y salida son obligatorias";
        }

        if (!datos.getFechaSalida().isAfter(datos.getFechaEntrada())) {
            return "La fecha de salida debe ser posterior a la entrada";
        }

        if (datos.getEstado() == null
                || !ESTADOS_VALIDOS.contains(
                        normalizarEstado(datos.getEstado())
                )) {
            return "El estado debe ser PENDIENTE, CONFIRMADA, "
                    + "CANCELADA o FINALIZADA";
        }

        if (datos.getTotal() == null) {
            return "El total es obligatorio";
        }

        if (datos.getTotal().signum() < 0) {
            return "El total no puede ser negativo";
        }

        if (datos.getTotal().stripTrailingZeros().scale() > 2) {
            return "El total debe tener como máximo dos decimales";
        }

        if (datos.getTotal().compareTo(
                new java.math.BigDecimal("99999999.99")
        ) > 0) {
            return "El total supera el máximo permitido";
        }

        return null;
    }

    private boolean existeCruce(
            Long habitacionId,
            LocalDate entradaNueva,
            LocalDate salidaNueva,
            Long reservaIdExcluir
    ) {
        return repo.findByHabitacionId(habitacionId)
                .stream()
                .filter(reserva ->
                        !Objects.equals(
                                reserva.getId(),
                                reservaIdExcluir
                        )
                )
                .filter(reserva ->
                        !"CANCELADA".equalsIgnoreCase(
                                reserva.getEstado()
                        )
                )
                .anyMatch(reserva ->
                        reserva.getFechaEntrada().isBefore(salidaNueva)
                                && reserva.getFechaSalida()
                                        .isAfter(entradaNueva)
                );
    }

    private String normalizarEstado(String estado) {
        return estado.trim().toUpperCase(Locale.ROOT);
    }
}