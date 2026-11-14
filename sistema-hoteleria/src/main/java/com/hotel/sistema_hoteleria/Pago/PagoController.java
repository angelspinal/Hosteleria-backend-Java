package com.hotel.sistema_hoteleria.Pago;

import com.hotel.sistema_hoteleria.Reserva.Reserva;
import com.hotel.sistema_hoteleria.Reserva.ReservaRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoRepository repo;
    private final ReservaRepository reservaRepo;

    private static final Set<String> METODOS_VALIDOS = Set.of(
            "EFECTIVO",
            "TARJETA",
            "TRANSFERENCIA"
    );

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "CONFIRMADO",
            "ANULADO"
    );

    @GetMapping("/listar")
    public List<Pago> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<Pago> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody Pago datos
    ) {
        return guardar(null, datos);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Pago datos
    ) {
        Pago existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if ("CONFIRMADO".equalsIgnoreCase(existente.getEstado())
                || "ANULADO".equalsIgnoreCase(existente.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Solo puedes modificar pagos pendientes. "
                                    + "Los pagos confirmados o anulados "
                                    + "deben conservar su registro"
                    );
        }

        return guardar(existente, datos);
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminar(
            @PathVariable("id") Long id
    ) {
        Pago existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"PENDIENTE".equalsIgnoreCase(existente.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Solo puedes eliminar pagos pendientes");
        }

        repo.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> guardar(
            Pago existente,
            Pago datos
    ) {
        String error = validarDatos(datos);

        if (error != null) {
            return ResponseEntity.badRequest().body(error);
        }

        Long reservaId = datos.getReserva().getId();

        if (existente != null
                && !Objects.equals(
                        existente.getReserva().getId(),
                        reservaId
                )) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("No puedes trasladar un pago a otra reserva");
        }

        Reserva reserva = reservaRepo.findById(reservaId)
                .orElse(null);

        if (reserva == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No existe una reserva con id: " + reservaId);
        }

        if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "No puedes registrar o modificar pagos "
                                    + "de una reserva cancelada"
                    );
        }

        String estado = normalizar(datos.getEstado());

        if ("CONFIRMADO".equals(estado)) {
            if (reserva.getTotal() == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("La reserva no tiene un total definido");
            }

            Long idExcluir = existente == null
                    ? null
                    : existente.getId();

            BigDecimal pagado = repo.findByReservaId(reservaId)
                    .stream()
                    .filter(pago ->
                            !Objects.equals(pago.getId(), idExcluir)
                    )
                    .filter(pago ->
                            "CONFIRMADO".equalsIgnoreCase(pago.getEstado())
                    )
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal saldo = reserva.getTotal().subtract(pagado);

            if (datos.getMonto().compareTo(saldo) > 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                "El monto supera el saldo pendiente "
                                        + "de la reserva: " + saldo
                        );
            }
        }

        boolean esNuevo = existente == null;

        Pago pago = esNuevo
                ? new Pago()
                : existente;

        pago.setReserva(reserva);
        pago.setMonto(datos.getMonto());
        pago.setMetodoPago(normalizar(datos.getMetodoPago()));
        pago.setFechaPago(datos.getFechaPago());
        pago.setEstado(estado);

        Pago guardado = repo.save(pago);

        if (esNuevo) {
            return ResponseEntity
                    .created(
                            URI.create(
                                    "/api/pagos/obtener/" + guardado.getId()
                            )
                    )
                    .body(guardado);
        }

        return ResponseEntity.ok(guardado);
    }

    private String validarDatos(Pago datos) {
        if (datos.getReserva() == null
                || datos.getReserva().getId() == null) {
            return "Debes indicar el id de la reserva";
        }

        if (datos.getMonto() == null
                || datos.getMonto().signum() <= 0) {
            return "El monto debe ser mayor que cero";
        }

        if (datos.getMonto().stripTrailingZeros().scale() > 2) {
            return "El monto debe tener como máximo dos decimales";
        }

        if (datos.getMonto().compareTo(
                new BigDecimal("99999999.99")
        ) > 0) {
            return "El monto supera el máximo permitido";
        }

        if (datos.getMetodoPago() == null
                || !METODOS_VALIDOS.contains(
                        normalizar(datos.getMetodoPago())
                )) {
            return "El método debe ser EFECTIVO, TARJETA o TRANSFERENCIA";
        }

        if (datos.getFechaPago() == null) {
            return "La fecha de pago es obligatoria";
        }

        if (datos.getEstado() == null
                || !ESTADOS_VALIDOS.contains(
                        normalizar(datos.getEstado())
                )) {
            return "El estado debe ser PENDIENTE, CONFIRMADO o ANULADO";
        }

        return null;
    }

    private String normalizar(String valor) {
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}