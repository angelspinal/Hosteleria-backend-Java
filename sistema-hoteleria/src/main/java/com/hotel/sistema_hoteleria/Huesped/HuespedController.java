package com.hotel.sistema_hoteleria.Huesped;

import com.hotel.sistema_hoteleria.Persona.Persona;
import com.hotel.sistema_hoteleria.Persona.PersonaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/huespedes")
@RequiredArgsConstructor
public class HuespedController {

    private final HuespedRepository repo;
    private final PersonaRepository personaRepo;

    @GetMapping("/listar")
    public List<Huesped> listar() {
        return repo.findAll();
    }

    @GetMapping("/obtener/{id}")
    public ResponseEntity<Huesped> obtener(
            @PathVariable("id") Long id
    ) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/crear")
    public ResponseEntity<?> crear(
            @Valid @RequestBody Huesped datos
    ) {
        Long personaId = datos.getPersona().getId();

        if (personaId == null) {
            return ResponseEntity.badRequest()
                    .body("Debes indicar el id de la persona");
        }

        Persona persona = personaRepo.findById(personaId)
                .orElse(null);

        if (persona == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            "No existe una persona con id: "
                                    + personaId
                    );
        }

        if (repo.existsByPersona_Id(personaId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Esta persona ya está registrada como huésped");
        }

        Huesped nuevo = new Huesped();
        nuevo.setPersona(persona);

        Huesped guardado = repo.save(nuevo);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/huespedes/obtener/"
                                        + guardado.getId()
                        )
                )
                .body(guardado);
    }

    @PutMapping("/actualizar/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable("id") Long id,
            @Valid @RequestBody Huesped datos
    ) {
        Huesped existente = repo.findById(id)
                .orElse(null);

        if (existente == null) {
            return ResponseEntity.notFound().build();
        }

        Long personaId = datos.getPersona().getId();

        if (personaId == null) {
            return ResponseEntity.badRequest()
                    .body("Debes indicar el id de la persona");
        }

        Persona persona = personaRepo.findById(personaId)
                .orElse(null);

        if (persona == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                            "No existe una persona con id: "
                                    + personaId
                    );
        }

        if (repo.existsByPersona_IdAndIdNot(personaId, id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Esta persona ya está vinculada a otro huésped"
                    );
        }

        existente.setPersona(persona);

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

        @PostMapping("/registrar")
        @Transactional
        public ResponseEntity<?> registrar(
                @Valid @RequestBody Persona datos
        ) {
        String documento = datos.getDocumento().trim();

        if (personaRepo.existsByDocumentoIgnoreCase(documento)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                "Ya existe una persona con ese documento. "
                                + "Debes vincular su registro existente, "
                                + "no crear otra persona."
                        );
        }

        Persona persona = new Persona();
        persona.setNombre(datos.getNombre().trim());
        persona.setApellido(datos.getApellido().trim());
        persona.setDocumento(documento);
        persona.setTelefono(datos.getTelefono().trim());
        persona.setCorreo(datos.getCorreo().trim());

        Persona guardada = personaRepo.save(persona);

        Huesped huesped = new Huesped();
        huesped.setPersona(guardada);

        Huesped registrado = repo.save(huesped);

        return ResponseEntity
                .created(
                        URI.create(
                                "/api/huespedes/obtener/"
                                        + registrado.getId()
                        )
                )
                .body(registrado);
        }

        @PutMapping("/{id}/datos-personales")
        @Transactional
        public ResponseEntity<?> actualizarDatosPersonales(
                @PathVariable("id") Long id,
                @Valid @RequestBody Persona datos
        ) {
        Huesped huesped = repo.findById(id).orElse(null);

        if (huesped == null) {
                return ResponseEntity.notFound().build();
        }

        Persona persona = huesped.getPersona();
        String documento = datos.getDocumento().trim();

        if (personaRepo.existsByDocumentoIgnoreCaseAndIdNot(
                documento,
                persona.getId()
        )) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(
                                "Ya existe otra persona con el documento: "
                                        + documento
                        );
        }

        persona.setNombre(datos.getNombre().trim());
        persona.setApellido(datos.getApellido().trim());
        persona.setDocumento(documento);
        persona.setTelefono(datos.getTelefono().trim());
        persona.setCorreo(datos.getCorreo().trim());

        personaRepo.save(persona);

        return ResponseEntity.ok(huesped);
        }
}