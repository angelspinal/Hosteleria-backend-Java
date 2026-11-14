package com.hotel.sistema_hoteleria.Persona;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaRepository repo;

    @GetMapping("/listar")
    public List<Persona> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<Persona> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody Persona datos
    ) {
        String documento = datos.getDocumento().trim();

        if (repo.existsByDocumentoIgnoreCase(documento)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe una persona con el documento: "
                                    + documento
                    );
        }

        datos.setId(null);
        datos.setNombre(datos.getNombre().trim());
        datos.setApellido(datos.getApellido().trim());
        datos.setDocumento(documento);
        datos.setTelefono(datos.getTelefono().trim());
        datos.setCorreo(datos.getCorreo().trim());

        Persona guardada = repo.save(datos);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/personas/obtener/"
                                        + guardada.getId()
                        )
                )
                .body(guardada);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Persona datos
    ) {
        Persona existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        String documento = datos.getDocumento().trim();

        if (repo.existsByDocumentoIgnoreCaseAndIdNot(documento, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Ya existe otra persona con el documento: "
                                    + documento
                    );
        }

        existente.setNombre(datos.getNombre().trim());
        existente.setApellido(datos.getApellido().trim());
        existente.setDocumento(documento);
        existente.setTelefono(datos.getTelefono().trim());
        existente.setCorreo(datos.getCorreo().trim());

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