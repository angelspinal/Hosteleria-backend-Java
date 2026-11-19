package com.hotel.sistema_hoteleria.Web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReservaVistaController {

    @GetMapping("/reservas")
    public String reservas() {
        return "reservas";
    }
}