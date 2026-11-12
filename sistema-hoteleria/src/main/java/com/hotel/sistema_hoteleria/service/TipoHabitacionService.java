package com.hotel.sistema_hoteleria.service;

import com.hotel.sistema_hoteleria.model.TipoHabitacion;
import com.hotel.sistema_hoteleria.repository.TipoHabitacionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TipoHabitacionService {

    private final TipoHabitacionRepository repository;

    public TipoHabitacionService(
            TipoHabitacionRepository repository
    ) {
        this.repository = repository;
    }

    public List<TipoHabitacion> listarTodos() {
        return repository.findAll();
    }

    public TipoHabitacion obtenerPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un tipo de habitación con id: " + id
                ));
    }

    @Transactional
    public TipoHabitacion crear(TipoHabitacion datos) {

        String nombre = datos.getNombre().trim();

        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un tipo de habitación con ese nombre"
            );
        }

        TipoHabitacion nuevo = new TipoHabitacion();

        nuevo.setNombre(nombre);
        nuevo.setDescripcion(datos.getDescripcion());
        nuevo.setPrecioBase(datos.getPrecioBase());
        nuevo.setCapacidad(datos.getCapacidad());

        return repository.save(nuevo);
    }

    @Transactional
    public TipoHabitacion actualizar(
            Long id,
            TipoHabitacion datos
    ) {
        TipoHabitacion existente = obtenerPorId(id);

        String nombre = datos.getNombre().trim();

        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otro tipo de habitación con ese nombre"
            );
        }

        existente.setNombre(nombre);
        existente.setDescripcion(datos.getDescripcion());
        existente.setPrecioBase(datos.getPrecioBase());
        existente.setCapacidad(datos.getCapacidad());

        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        TipoHabitacion existente = obtenerPorId(id);
        repository.delete(existente);
    }
}