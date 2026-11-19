package com.hotel.sistema_hoteleria.Web;

import com.hotel.sistema_hoteleria.Habitacion.Habitacion;
import com.hotel.sistema_hoteleria.TipoHabitacion.TipoHabitacionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HabitacionVistaController {

    private final TipoHabitacionRepository tipoRepo;

    @GetMapping("/")
    public String inicio() {
        return "redirect:/habitaciones";
    }

    @GetMapping("/habitaciones")
    public String habitaciones(Model model) {
        model.addAttribute(
                "tiposHabitacion",
                tipoRepo.findAll()
        );

        model.addAttribute(
                "estados",
                Habitacion.Estado.values()
        );

        return "habitaciones";
    }
}