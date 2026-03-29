package com.buraqai.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/employee/test")
    public String employeeTest() {
        return "Employee access confirmed";
    }

    @GetMapping("/api/admin/test")
    public String adminTest() {
        return "Admin access confirmed";
    }
}