package com.hotel.sistema_hoteleria.Web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HuespedVistaController {

    @GetMapping("/huespedes")
    public String huespedes() {
        return "huespedes";
    }
}