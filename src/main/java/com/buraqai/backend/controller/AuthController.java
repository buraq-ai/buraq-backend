package com.buraqai.backend.controller;

import com.buraqai.backend.dto.LoginRequestDTO;
import com.buraqai.backend.dto.LoginResponseDTO;
import com.buraqai.backend.dto.LogoutRequestDTO;
import com.buraqai.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import com.buraqai.backend.exception.AccountDeactivatedException;



@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, BCryptPasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            LoginResponseDTO response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (AccountDeactivatedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", 401, "message", "Invalid email or password"));
        }
    }

    @GetMapping("/generate-hash")
    public String generateHash(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutRequestDTO request) {
        try {
            authService.logout(request.getRefreshToken());
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        if (ex instanceof AccountDeactivatedException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", 403, "message", ex.getMessage()));
        }
        if (ex.getMessage() != null && ex.getMessage().equals("Refresh token not found")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Refresh token not found"));
        }
        // Invalid credentials or any other auth error → 401
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", 401, "message", "Invalid email or password"));
    }


}