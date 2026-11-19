package com.hotel.sistema_hoteleria.Config;

import com.hotel.sistema_hoteleria.Habitacion.Habitacion;
import com.hotel.sistema_hoteleria.Habitacion.HabitacionRepository;
import com.hotel.sistema_hoteleria.Huesped.Huesped;
import com.hotel.sistema_hoteleria.Huesped.HuespedRepository;
import com.hotel.sistema_hoteleria.Persona.Persona;
import com.hotel.sistema_hoteleria.Persona.PersonaRepository;
import com.hotel.sistema_hoteleria.Reserva.Reserva;
import com.hotel.sistema_hoteleria.Reserva.ReservaRepository;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuesped;
import com.hotel.sistema_hoteleria.ReservaHuesped.ReservaHuespedRepository;
import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacion;
import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacionRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Configuration
public class DatosIniciales {

    @Bean
    CommandLineRunner cargarDatosDemostracion(
            TipoHabitacionRepository tipoRepo,
            HabitacionRepository habitacionRepo,
            PersonaRepository personaRepo,
            HuespedRepository huespedRepo,
            ReservaRepository reservaRepo,
            ReservaHuespedRepository vinculacionRepo,
            TransactionTemplate transactionTemplate
    ) {
        return args -> {
            transactionTemplate.executeWithoutResult(status -> {

                List<TipoHabitacion> tipos = cargarTipos(tipoRepo);

                List<Habitacion> habitaciones = cargarHabitaciones(
                        habitacionRepo,
                        tipos
                );

                List<Huesped> huespedes = cargarHuespedes(
                        personaRepo,
                        huespedRepo
                );

                cargarReservas(
                        reservaRepo,
                        vinculacionRepo,
                        habitaciones,
                        huespedes
                );
            });

            System.out.println();
            System.out.println("========================================");
            System.out.println("DATOS DE DEMOSTRACIÓN CARGADOS");
            System.out.println("Tipos: " + tipoRepo.count());
            System.out.println("Habitaciones: " + habitacionRepo.count());
            System.out.println("Personas: " + personaRepo.count());
            System.out.println("Huéspedes: " + huespedRepo.count());
            System.out.println("Reservas: " + reservaRepo.count());
            System.out.println(
                    "Vinculaciones reserva-huésped: "
                            + vinculacionRepo.count()
            );
            System.out.println("========================================");
            System.out.println();
        };
    }

    private List<TipoHabitacion> cargarTipos(
            TipoHabitacionRepository repo
    ) {
        String[] nombres = {
                "Individual",
                "Doble",
                "Triple",
                "Familiar",
                "Suite"
        };

        String[] descripciones = {
                "Habitación para una persona",
                "Habitación para dos personas",
                "Habitación para tres personas",
                "Habitación para cuatro personas",
                "Suite para dos personas"
        };

        double[] precios = {
                35.00,
                60.00,
                85.00,
                110.00,
                150.00
        };

        int[] capacidades = {
                1,
                2,
                3,
                4,
                2
        };

        List<TipoHabitacion> existentes =
                new ArrayList<>(repo.findAll());

        List<TipoHabitacion> resultado = new ArrayList<>();

        for (int i = 0; i < nombres.length; i++) {
            String nombre = nombres[i];

            TipoHabitacion tipo = existentes.stream()
                    .filter(t ->
                            nombre.equalsIgnoreCase(
                                    t.getNombre().trim()
                            )
                    )
                    .findFirst()
                    .orElse(null);

            if (tipo == null) {
                tipo = new TipoHabitacion();
                tipo.setNombre(nombre);
                tipo.setDescripcion(descripciones[i]);
                tipo.setPrecioBase(precios[i]);
                tipo.setCapacidad(capacidades[i]);

                tipo = repo.save(tipo);
                existentes.add(tipo);
            }

            resultado.add(tipo);
        }

        return resultado;
    }

    private List<Habitacion> cargarHabitaciones(
            HabitacionRepository repo,
            List<TipoHabitacion> tipos
    ) {
        String[] numeros = {
                "DEMO-101",
                "DEMO-102",
                "DEMO-201",
                "DEMO-202",
                "DEMO-301"
        };

        int[] pisos = {
                1,
                1,
                2,
                2,
                3
        };

        List<Habitacion> existentes =
                new ArrayList<>(repo.findAll());

        List<Habitacion> resultado = new ArrayList<>();

        for (int i = 0; i < numeros.length; i++) {
            String numero = numeros[i];

            Habitacion habitacion = existentes.stream()
                    .filter(h ->
                            numero.equalsIgnoreCase(
                                    h.getNumero().trim()
                            )
                    )
                    .findFirst()
                    .orElse(null);

            if (habitacion == null) {
                habitacion = new Habitacion();
                habitacion.setNumero(numero);
                habitacion.setPiso(pisos[i]);
                habitacion.setEstado(
                        Habitacion.Estado.HABILITADA
                );
                habitacion.setTipoHabitacion(tipos.get(i));

                habitacion = repo.save(habitacion);
                existentes.add(habitacion);
            }

            resultado.add(habitacion);
        }

        return resultado;
    }

    private List<Huesped> cargarHuespedes(
            PersonaRepository personaRepo,
            HuespedRepository huespedRepo
    ) {
        String[] nombres = {
                "Ana",
                "Carlos",
                "María",
                "Luis",
                "Sofía"
        };

        String[] apellidos = {
                "López",
                "Mendoza",
                "García",
                "Hernández",
                "Martínez"
        };

        // Identificadores y contactos ficticios para pruebas.
        String[] documentos = {
                "DEMO-H-001",
                "DEMO-H-002",
                "DEMO-H-003",
                "DEMO-H-004",
                "DEMO-H-005"
        };

        String[] telefonos = {
                "00000001",
                "00000002",
                "00000003",
                "00000004",
                "00000005"
        };

        String[] correos = {
                "ana.demo@example.com",
                "carlos.demo@example.com",
                "maria.demo@example.com",
                "luis.demo@example.com",
                "sofia.demo@example.com"
        };

        List<Persona> personas =
                new ArrayList<>(personaRepo.findAll());

        List<Huesped> existentes =
                new ArrayList<>(huespedRepo.findAll());

        List<Huesped> resultado = new ArrayList<>();

        for (int i = 0; i < documentos.length; i++) {
            String documento = documentos[i];

            Persona persona = personas.stream()
                    .filter(p ->
                            documento.equalsIgnoreCase(
                                    p.getDocumento().trim()
                            )
                    )
                    .findFirst()
                    .orElse(null);

            if (persona == null) {
                persona = new Persona();
                persona.setNombre(nombres[i]);
                persona.setApellido(apellidos[i]);
                persona.setDocumento(documento);
                persona.setTelefono(telefonos[i]);
                persona.setCorreo(correos[i]);

                persona = personaRepo.save(persona);
                personas.add(persona);
            }

            Long personaId = persona.getId();

            Huesped huesped = existentes.stream()
                    .filter(h ->
                            Objects.equals(
                                    h.getPersona().getId(),
                                    personaId
                            )
                    )
                    .findFirst()
                    .orElse(null);

            if (huesped == null) {
                huesped = new Huesped();
                huesped.setPersona(persona);

                huesped = huespedRepo.save(huesped);
                existentes.add(huesped);
            }

            resultado.add(huesped);
        }

        return resultado;
    }

    private void cargarReservas(
            ReservaRepository reservaRepo,
            ReservaHuespedRepository vinculacionRepo,
            List<Habitacion> habitaciones,
            List<Huesped> huespedes
    ) {
        LocalDate fechaBase = LocalDate.now().plusDays(14);

        for (int i = 0; i < habitaciones.size(); i++) {
            Habitacion habitacion = habitaciones.get(i);
            Huesped huesped = huespedes.get(i);

            // Reconocer la reserva de demostración por su pareja
            // habitación-huésped, aunque se haya cancelado.
            boolean yaExiste = vinculacionRepo
                    .findByHuespedId(huesped.getId())
                    .stream()
                    .anyMatch(v ->
                            Objects.equals(
                                    v.getReserva()
                                            .getHabitacion()
                                            .getId(),
                                    habitacion.getId()
                            )
                    );

            if (yaExiste) {
                continue;
            }

            if (habitacion.getEstado()
                    != Habitacion.Estado.HABILITADA) {
                throw new IllegalStateException(
                        "La habitación de demostración "
                                + habitacion.getNumero()
                                + " no está habilitada. "
                                + "No se modificó su estado automáticamente."
                );
            }

            Integer capacidad =
                    habitacion.getTipoHabitacion().getCapacidad();

            if (capacidad == null || capacidad < 1) {
                throw new IllegalStateException(
                        "La habitación "
                                + habitacion.getNumero()
                                + " no tiene capacidad válida."
                );
            }

            LocalDate entrada = fechaBase.plusDays(i * 3L);
            LocalDate salida = entrada.plusDays(2);

            // Buscar otro período si ya hay una reserva.
            int intentos = 0;

            while (hayCruce(
                    reservaRepo,
                    habitacion.getId(),
                    entrada,
                    salida
            )) {
                entrada = entrada.plusDays(3);
                salida = entrada.plusDays(2);

                intentos++;

                if (intentos > 365) {
                    throw new IllegalStateException(
                            "No se encontró un período libre para "
                                    + habitacion.getNumero()
                    );
                }
            }

            Double precioBase =
                    habitacion.getTipoHabitacion().getPrecioBase();

            if (precioBase == null
                    || !Double.isFinite(precioBase)
                    || precioBase <= 0) {
                throw new IllegalStateException(
                        "Precio inválido para "
                                + habitacion.getNumero()
                );
            }

            BigDecimal precio = BigDecimal.valueOf(precioBase)
                    .setScale(2, RoundingMode.HALF_UP);

            long noches = ChronoUnit.DAYS.between(
                    entrada,
                    salida
            );

            BigDecimal total = precio
                    .multiply(BigDecimal.valueOf(noches))
                    .setScale(2, RoundingMode.HALF_UP);

            if (precio.signum() <= 0
                    || total.compareTo(
                            new BigDecimal("99999999.99")
                    ) > 0) {
                throw new IllegalStateException(
                        "El total no es válido para "
                                + habitacion.getNumero()
                );
            }

            Reserva reserva = new Reserva();
            reserva.setHabitacion(habitacion);
            reserva.setFechaEntrada(entrada);
            reserva.setFechaSalida(salida);
            reserva.setEstado(
                    i % 2 == 0 ? "CONFIRMADA" : "PENDIENTE"
            );
            reserva.setTotal(total);

            reserva = reservaRepo.save(reserva);

            ReservaHuesped vinculacion = new ReservaHuesped();
            vinculacion.setReserva(reserva);
            vinculacion.setHuesped(huesped);
            vinculacion.setFechaEntrada(entrada);
            vinculacion.setFechaSalida(salida);

            vinculacionRepo.save(vinculacion);

            System.out.println(
                    ">>> Reserva demo: "
                            + habitacion.getNumero()
                            + " | "
                            + entrada
                            + " al "
                            + salida
                            + " | Total: "
                            + total
            );
        }
    }

    private boolean hayCruce(
            ReservaRepository repo,
            Long habitacionId,
            LocalDate entrada,
            LocalDate salida
    ) {
        return repo.findByHabitacionId(habitacionId)
                .stream()
                .filter(r ->
                        !"CANCELADA".equalsIgnoreCase(
                                r.getEstado() == null
                                        ? ""
                                        : r.getEstado().trim()
                        )
                )
                .anyMatch(r ->
                        r.getFechaEntrada().isBefore(salida)
                                && r.getFechaSalida().isAfter(entrada)
                );
    }
}