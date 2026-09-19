package com.zeqi.user.controller;

import com.zeqi.user.dto.LoginRequest;
import com.zeqi.user.dto.LoginResponse;
import com.zeqi.user.dto.MessageResponse;
import com.zeqi.user.dto.RegisterRequest;
import com.zeqi.user.dto.UserResponse;
import com.zeqi.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest r) {
        return ResponseEntity.status(201).body(service.register(r));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r) {
        return service.login(r);
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest r) {
        return service.logout((Long) r.getAttribute("userId"));
    }
}
