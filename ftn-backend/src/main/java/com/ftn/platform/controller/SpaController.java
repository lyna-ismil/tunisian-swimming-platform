package com.ftn.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/landing",
        "/landing/**",
        "/admin",
        "/admin/**",
        "/admin-login"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
