package com.managehouse.money.controller;

import com.managehouse.money.dto.LoginRequest;
import com.managehouse.money.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.getEmail())
                .map(user -> ResponseEntity.ok((Object) user))
                .orElse(ResponseEntity.status(401).body(
                        new ErrorResponse("Email não encontrado")
                ));
    }

    private record ErrorResponse(String message) {}
}

