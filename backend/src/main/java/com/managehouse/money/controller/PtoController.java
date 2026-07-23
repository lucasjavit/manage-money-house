package com.managehouse.money.controller;

import com.managehouse.money.dto.*;
import com.managehouse.money.service.PtoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pto")
@RequiredArgsConstructor
public class PtoController {

    private final PtoService ptoService;

    @PostMapping("/config")
    public ResponseEntity<PtoConfigResponse> saveConfig(@RequestBody PtoConfigRequest request) {
        return ResponseEntity.ok(ptoService.createOrUpdateConfig(request));
    }

    @GetMapping("/config")
    public ResponseEntity<PtoConfigResponse> getConfig(@RequestParam Long userId) {
        return ResponseEntity.ok(ptoService.getConfig(userId));
    }

    @PostMapping("/vacations")
    public ResponseEntity<PtoVacationResponse> createVacation(@RequestBody PtoVacationRequest request) {
        return ResponseEntity.ok(ptoService.createVacation(request));
    }

    @GetMapping("/vacations")
    public ResponseEntity<List<PtoVacationResponse>> getVacations(@RequestParam Long userId) {
        return ResponseEntity.ok(ptoService.getVacations(userId));
    }

    @DeleteMapping("/vacations/{id}")
    public ResponseEntity<Void> deleteVacation(@PathVariable Long id) {
        ptoService.deleteVacation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/balance")
    public ResponseEntity<PtoBalanceResponse> getBalance(
            @RequestParam Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ptoService.getBalance(userId, date));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handle(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
