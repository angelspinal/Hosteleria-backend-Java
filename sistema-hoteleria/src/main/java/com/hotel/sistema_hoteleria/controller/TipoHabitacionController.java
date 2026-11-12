package com.hotel.sistema_hoteleria.controller;

import com.hotel.sistema_hoteleria.model.TipoHabitacion;
import com.hotel.sistema_hoteleria.service.TipoHabitacionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-habitacion")
public class TipoHabitacionController {

    private final TipoHabitacionService service;

    public TipoHabitacionController(
            TipoHabitacionService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TipoHabitacion>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoHabitacion> obtenerPorId(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<TipoHabitacion> crear(
            @Valid @RequestBody TipoHabitacion datos
    ) {
        TipoHabitacion creado = service.crear(datos);

        URI ubicacion = URI.create(
                "/api/tipos-habitacion/" + creado.getId()
        );

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoHabitacion> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody TipoHabitacion datos
    ) {
        return ResponseEntity.ok(service.actualizar(id, datos));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable("id") Long id
    ) {
        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}