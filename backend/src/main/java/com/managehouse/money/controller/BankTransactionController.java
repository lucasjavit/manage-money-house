package com.managehouse.money.controller;

import com.managehouse.money.dto.BankTransactionResponse;
import com.managehouse.money.entity.BankTransaction;
import com.managehouse.money.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gastos pessoais capturados pelo app (aba "Lucas - Gastos"). Listagem por mês,
 * conclusão de pendentes (informar o valor) e exclusão.
 */
@RestController
@RequestMapping("/api/bank-transactions")
@RequiredArgsConstructor
public class BankTransactionController {

    private final BankTransactionRepository repository;

    @GetMapping
    public ResponseEntity<List<BankTransactionResponse>> list(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        List<BankTransactionResponse> result = repository
                .findByUserAndMonth(userId, year, month).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** Completa um pendente: informa o valor e (opcional) a descrição; sai de needs_review. */
    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<BankTransactionResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateRequest body) {
        BankTransaction tx = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transação não encontrada: " + id));

        if (body.amount() != null) {
            if (body.amount().signum() <= 0) {
                throw new IllegalArgumentException("amount deve ser maior que zero");
            }
            tx.setAmount(body.amount());
            tx.setNeedsReview(false);
        }
        if (body.description() != null && !body.description().isBlank()) {
            tx.setDescription(body.description().trim());
        }
        return ResponseEntity.ok(toResponse(repository.save(tx)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handle(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }

    private BankTransactionResponse toResponse(BankTransaction b) {
        return new BankTransactionResponse(
                b.getId(),
                b.getAmount(),
                b.getDescription(),
                b.getTransactionDate(),
                b.getSourcePackage(),
                b.getRawText(),
                b.isNeedsReview());
    }

    public record UpdateRequest(BigDecimal amount, String description) {}
}
