package com.hotel.sistema_hoteleria.Reserva;

import com.hotel.sistema_hoteleria.Habitacion.Habitacion;
import com.hotel.sistema_hoteleria.Habitacion.HabitacionRepository;
import com.hotel.sistema_hoteleria.Huesped.Huesped;
import com.hotel.sistema_hoteleria.Huesped.HuespedRepository;
import com.hotel.sistema_hoteleria.Pago.PagoRepository;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuesped;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuespedRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaRepository repo;
    private final HabitacionRepository habitacionRepo;
    private final HuespedRepository huespedRepo;
    private final ReservaHuespedRepository reservaHuespedRepo;
    private final PagoRepository pagoRepo;

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE",
            "CONFIRMADA",
            "CANCELADA",
            "FINALIZADA"
    );

    private static final Set<String> ESTADOS_INICIALES = Set.of(
            "PENDIENTE",
            "CONFIRMADA"
    );

    private static final BigDecimal TOTAL_MAXIMO =
            new BigDecimal("99999999.99");

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

    @GetMapping("/{id}/huespedes")
    @Transactional(readOnly = true)
    public List<ReservaHuesped> obtenerHuespedes(
            @PathVariable("id") Long id
    ) {
        buscarReserva(id);
        return reservaHuespedRepo.findByReservaId(id);
    }

    @GetMapping("/disponibles")
    @Transactional(readOnly = true)
    public List<HabitacionDisponible> disponibles(
            @RequestParam("entrada")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate entrada,

            @RequestParam("salida")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate salida
    ) {
        validarFechas(entrada, salida);

        long noches = ChronoUnit.DAYS.between(entrada, salida);
        List<HabitacionDisponible> resultado = new ArrayList<>();

        for (Habitacion habitacion : habitacionRepo.findAll()) {
            if (habitacion.getEstado()
                    != Habitacion.Estado.HABILITADA) {
                continue;
            }

            if (repo.contarCruces(
                    habitacion.getId(),
                    entrada,
                    salida,
                    null
            ) > 0) {
                continue;
            }

            BigDecimal precio = obtenerPrecio(habitacion);
            BigDecimal total = precio.multiply(
                    BigDecimal.valueOf(noches)
            ).setScale(2, RoundingMode.HALF_UP);

            // No ofrecer un período cuyo total no cabe en la columna.
            if (total.compareTo(TOTAL_MAXIMO) > 0) {
                continue;
            }

            resultado.add(
                    new HabitacionDisponible(
                            habitacion.getId(),
                            habitacion.getNumero(),
                            habitacion.getPiso(),
                            habitacion.getTipoHabitacion().getNombre(),
                            habitacion.getTipoHabitacion().getCapacidad(),
                            precio,
                            noches,
                            total
                    )
            );
        }

        return resultado;
    }

    // Ruta existente: crea solo la reserva.
    // El total recibido del cliente se ignora y se calcula aquí.
    @PostMapping("/crear")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Reserva> crear(
            @Valid @RequestBody Reserva datos
    ) {
        if (datos.getHabitacion() == null
                || datos.getHabitacion().getId() == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el id de la habitación"
            );
        }

        Reserva guardada = crearReserva(
                datos.getHabitacion().getId(),
                datos.getFechaEntrada(),
                datos.getFechaSalida(),
                datos.getEstado()
        );

        return respuestaCreada(guardada);
    }

    // Ruta para el formulario: reserva y huéspedes juntos.
    @PostMapping("/registrar")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Reserva> registrar(
            @Valid @RequestBody ReservaRegistro datos
    ) {
        validarFechas(
                datos.fechaEntrada(),
                datos.fechaSalida()
        );

        String estado = estadoInicial(datos.estado());

        Set<Long> idsUnicos = new HashSet<>(datos.huespedIds());

        if (idsUnicos.size() != datos.huespedIds().size()) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "No puedes seleccionar dos veces al mismo huésped"
            );
        }

        Habitacion habitacion = buscarHabitacion(
                datos.habitacionId()
        );

        validarCapacidad(habitacion, idsUnicos.size());

        List<Huesped> huespedes =
                huespedRepo.findAllById(idsUnicos);

        if (huespedes.size() != idsUnicos.size()) {
            throw error(
                    HttpStatus.NOT_FOUND,
                    "Uno o más huéspedes seleccionados no existen"
            );
        }

        Reserva reserva = crearReserva(
                habitacion.getId(),
                datos.fechaEntrada(),
                datos.fechaSalida(),
                estado
        );

        List<ReservaHuesped> vinculaciones = new ArrayList<>();

        for (Huesped huesped : huespedes) {
            ReservaHuesped vinculacion = new ReservaHuesped();

            vinculacion.setReserva(reserva);
            vinculacion.setHuesped(huesped);
            vinculacion.setFechaEntrada(datos.fechaEntrada());
            vinculacion.setFechaSalida(datos.fechaSalida());

            vinculaciones.add(vinculacion);
        }

        reservaHuespedRepo.saveAll(vinculaciones);

        return respuestaCreada(reserva);
    }

    @PutMapping("/actualizar/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Reserva> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Reserva datos
    ) {
        Reserva existente = buscarReserva(id);

        String estadoActual = normalizarEstado(
                existente.getEstado()
        );

        if ("CANCELADA".equals(estadoActual)
                || "FINALIZADA".equals(estadoActual)) {
            throw error(
                    HttpStatus.CONFLICT,
                    "No puedes modificar una reserva cancelada o finalizada"
            );
        }

        validarFechas(
                datos.getFechaEntrada(),
                datos.getFechaSalida()
        );

        if (datos.getHabitacion() == null
                || datos.getHabitacion().getId() == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Debes indicar el id de la habitación"
            );
        }

        String estadoNuevo = normalizarEstado(datos.getEstado());

        if (!ESTADOS_VALIDOS.contains(estadoNuevo)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Estado de reserva no válido"
            );
        }

        boolean cambiaPeriodoOHabitacion =
                !existente.getHabitacion().getId().equals(
                        datos.getHabitacion().getId()
                )
                || !existente.getFechaEntrada().equals(
                        datos.getFechaEntrada()
                )
                || !existente.getFechaSalida().equals(
                        datos.getFechaSalida()
                );

        if (cambiaPeriodoOHabitacion
                && !pagoRepo.findByReservaId(id).isEmpty()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "No puedes cambiar habitación o fechas "
                            + "de una reserva con pagos registrados"
            );
        }

        // Cancelar o finalizar no debe cambiar fechas ni habitación.
        if (("CANCELADA".equals(estadoNuevo)
                || "FINALIZADA".equals(estadoNuevo))
                && cambiaPeriodoOHabitacion) {
            throw error(
                    HttpStatus.CONFLICT,
                    "Para cancelar o finalizar, conserva "
                            + "la habitación y las fechas originales"
            );
        }

        if ("CANCELADA".equals(estadoNuevo)) {
            existente.setEstado("CANCELADA");
            return ResponseEntity.ok(repo.save(existente));
        }

        if ("FINALIZADA".equals(estadoNuevo)) {
            if (LocalDate.now().isBefore(existente.getFechaSalida())) {
                throw error(
                        HttpStatus.CONFLICT,
                        "No puedes finalizar antes de la fecha de salida"
                );
            }

            existente.setEstado("FINALIZADA");
            return ResponseEntity.ok(repo.save(existente));
        }

        Habitacion habitacion = buscarHabitacion(
                datos.getHabitacion().getId()
        );

        // Cambiar solo PENDIENTE/CONFIRMADA no recalcula el precio.
        if (cambiaPeriodoOHabitacion) {
            validarHabitacionHabilitada(habitacion);
        }

        validarSinCruces(
                habitacion.getId(),
                datos.getFechaEntrada(),
                datos.getFechaSalida(),
                id
        );

        List<ReservaHuesped> vinculaciones =
                reservaHuespedRepo.findByReservaId(id);

        validarCapacidad(habitacion, vinculaciones.size());

        for (ReservaHuesped vinculacion : vinculaciones) {
            LocalDate entrada = vinculacion.getFechaEntrada();
            LocalDate salida = vinculacion.getFechaSalida();

            if (entrada == null
                    || salida == null
                    || !salida.isAfter(entrada)
                    || entrada.isBefore(datos.getFechaEntrada())
                    || salida.isAfter(datos.getFechaSalida())) {
                throw error(
                        HttpStatus.CONFLICT,
                        "Las fechas de un huésped quedan fuera "
                                + "del nuevo período de la reserva"
                );
            }
        }

        if (cambiaPeriodoOHabitacion) {
            existente.setTotal(
                    calcularTotal(
                            habitacion,
                            datos.getFechaEntrada(),
                            datos.getFechaSalida()
                    )
            );
        }

        existente.setHabitacion(habitacion);
        existente.setFechaEntrada(datos.getFechaEntrada());
        existente.setFechaSalida(datos.getFechaSalida());
        existente.setEstado(estadoNuevo);

        return ResponseEntity.ok(repo.save(existente));
    }

    @PutMapping("/{id}/cancelar")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Reserva> cancelar(
            @PathVariable("id") Long id
    ) {
        Reserva reserva = buscarReserva(id);

        if ("FINALIZADA".equals(
                normalizarEstado(reserva.getEstado())
        )) {
            throw error(
                    HttpStatus.CONFLICT,
                    "No puedes cancelar una reserva finalizada"
            );
        }

        // Conserva total, huéspedes y pagos como historial.
        reserva.setEstado("CANCELADA");

        return ResponseEntity.ok(repo.save(reserva));
    }

    @DeleteMapping("/eliminar/{id}")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseEntity<Void> eliminar(
            @PathVariable("id") Long id
    ) {
        buscarReserva(id);

        if (!reservaHuespedRepo.findByReservaId(id).isEmpty()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "No puedes eliminar una reserva "
                            + "que tiene huéspedes asociados"
            );
        }

        if (!pagoRepo.findByReservaId(id).isEmpty()) {
            throw error(
                    HttpStatus.CONFLICT,
                    "No puedes eliminar una reserva "
                            + "que tiene pagos registrados"
            );
        }

        repo.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    private Reserva crearReserva(
            Long habitacionId,
            LocalDate entrada,
            LocalDate salida,
            String estadoSolicitado
    ) {
        validarFechas(entrada, salida);

        String estado = estadoInicial(estadoSolicitado);

        Habitacion habitacion = buscarHabitacion(habitacionId);

        validarHabitacionHabilitada(habitacion);

        validarSinCruces(
                habitacionId,
                entrada,
                salida,
                null
        );

        Reserva reserva = new Reserva();
        reserva.setHabitacion(habitacion);
        reserva.setFechaEntrada(entrada);
        reserva.setFechaSalida(salida);
        reserva.setEstado(estado);
        reserva.setTotal(
                calcularTotal(habitacion, entrada, salida)
        );

        return repo.save(reserva);
    }

    private Reserva buscarReserva(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "No existe una reserva con id: " + id
                ));
    }

    private Habitacion buscarHabitacion(Long id) {
        return habitacionRepo.findById(id)
                .orElseThrow(() -> error(
                        HttpStatus.NOT_FOUND,
                        "No existe una habitación con id: " + id
                ));
    }

    private void validarHabitacionHabilitada(
            Habitacion habitacion
    ) {
        if (habitacion.getEstado()
                != Habitacion.Estado.HABILITADA) {
            throw error(
                    HttpStatus.CONFLICT,
                    "La habitación no está habilitada para reservar"
            );
        }
    }

    private void validarCapacidad(
            Habitacion habitacion,
            int cantidad
    ) {
        Integer capacidad =
                habitacion.getTipoHabitacion().getCapacidad();

        if (capacidad == null || capacidad < 1) {
            throw error(
                    HttpStatus.CONFLICT,
                    "El tipo de habitación no tiene una capacidad válida"
            );
        }

        if (cantidad > capacidad) {
            throw error(
                    HttpStatus.CONFLICT,
                    "La habitación admite como máximo "
                            + capacidad + " huésped(es)"
            );
        }
    }

    private void validarSinCruces(
            Long habitacionId,
            LocalDate entrada,
            LocalDate salida,
            Long excluirId
    ) {
        if (repo.contarCruces(
                habitacionId,
                entrada,
                salida,
                excluirId
        ) > 0) {
            throw error(
                    HttpStatus.CONFLICT,
                    "La habitación ya tiene una reserva en esas fechas"
            );
        }
    }

    private void validarFechas(
            LocalDate entrada,
            LocalDate salida
    ) {
        if (entrada == null || salida == null) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Las fechas de entrada y salida son obligatorias"
            );
        }

        if (!salida.isAfter(entrada)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "La fecha de salida debe ser posterior a la entrada"
            );
        }
    }

    private BigDecimal obtenerPrecio(
            Habitacion habitacion
    ) {
        Double precio =
                habitacion.getTipoHabitacion().getPrecioBase();

        if (precio == null
                || !Double.isFinite(precio)
                || precio <= 0) {
            throw error(
                    HttpStatus.CONFLICT,
                    "El tipo de habitación no tiene un precio válido"
            );
        }

        BigDecimal precioRedondeado = BigDecimal.valueOf(precio)
                .setScale(2, RoundingMode.HALF_UP);

        if (precioRedondeado.signum() <= 0) {
            throw error(
                    HttpStatus.CONFLICT,
                    "El precio por noche debe ser al menos 0.01"
            );
        }

        return precioRedondeado;
    }

    private BigDecimal calcularTotal(
            Habitacion habitacion,
            LocalDate entrada,
            LocalDate salida
    ) {
        long noches = ChronoUnit.DAYS.between(entrada, salida);

        BigDecimal total = obtenerPrecio(habitacion)
                .multiply(BigDecimal.valueOf(noches))
                .setScale(2, RoundingMode.HALF_UP);

        if (total.compareTo(TOTAL_MAXIMO) > 0) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "El total supera el máximo permitido"
            );
        }

        return total;
    }

    private String estadoInicial(String estado) {
        String normalizado = estado == null || estado.isBlank()
                ? "PENDIENTE"
                : normalizarEstado(estado);

        if (!ESTADOS_INICIALES.contains(normalizado)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Una reserva nueva debe ser PENDIENTE o CONFIRMADA"
            );
        }

        return normalizado;
    }

    private String normalizarEstado(String estado) {
        return estado == null
                ? ""
                : estado.trim().toUpperCase(Locale.ROOT);
    }

    private ResponseEntity<Reserva> respuestaCreada(
            Reserva reserva
    ) {
        return ResponseEntity
                .created(
                        URI.create(
                                "/api/reservas/obtener/"
                                        + reserva.getId()
                        )
                )
                .body(reserva);
    }

    private ResponseStatusException error(
            HttpStatus estado,
            String mensaje
    ) {
        return new ResponseStatusException(estado, mensaje);
    }
}