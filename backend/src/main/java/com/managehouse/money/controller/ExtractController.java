package com.managehouse.money.controller;

import com.managehouse.money.dto.ExtractProcessResponse;
import com.managehouse.money.dto.ExtractTransactionResponse;
import com.managehouse.money.dto.ExtractUploadRequest;
import com.managehouse.money.dto.ExpenseInsightsResponse;
import com.managehouse.money.dto.IdentifiedTransaction;
import com.managehouse.money.dto.SaveTransactionsRequest;
import com.managehouse.money.service.ExpenseInsightsService;
import com.managehouse.money.service.ExtractService;
import com.managehouse.money.service.ExtractTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/extract")
@RequiredArgsConstructor
@Slf4j
public class ExtractController {
    private final ExtractService extractService;
    private final ExtractTransactionService extractTransactionService;
    private final ExpenseInsightsService expenseInsightsService;
    
    @PostMapping("/process")
    public ResponseEntity<ExtractProcessResponse> processExtract(
            @RequestBody ExtractUploadRequest request) {
        ExtractProcessResponse response = extractService.processExtract(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/save")
    public ResponseEntity<?> saveTransactions(@RequestBody SaveTransactionsRequest request) {
        try {
            log.info("Recebendo requisição para salvar transações. UserId: {}, Quantidade: {}", request.getUserId(), request.getTransactions().size());
            
            // Validar todas as transações antes de salvar
            List<String> errors = new java.util.ArrayList<>();
            List<IdentifiedTransaction> validTransactions = new java.util.ArrayList<>();
            
            for (IdentifiedTransaction transaction : request.getTransactions()) {
                log.debug("Validando transação: {}, Data: {}, Valor: {}", transaction.getDescription(), transaction.getDate(), transaction.getAmount());
                if (validateTransaction(transaction)) {
                    validTransactions.add(transaction);
                    log.debug("Transação válida: {}", transaction.getDescription());
                } else {
                    String errorMsg = "Transação inválida: " + transaction.getDescription();
                    errors.add(errorMsg);
                    log.warn(errorMsg);
                }
            }
            
            if (validTransactions.isEmpty()) {
                log.warn("Nenhuma transação válida para salvar");
                return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "Nenhuma transação válida para salvar", "errors", errors)
                );
            }
            
            // Salvar transações do extrato (não na planilha principal)
            log.info("Salvando {} transações válidas", validTransactions.size());
            List<ExtractTransactionResponse> savedTransactions = 
                extractTransactionService.saveTransactions(request.getUserId(), validTransactions);
            
            log.info("Transações salvas com sucesso: {}", savedTransactions.size());
            
            return ResponseEntity.ok(java.util.Map.of(
                "saved", savedTransactions.size(),
                "failed", errors.size(),
                "errors", errors,
                "transactions", savedTransactions
            ));
            
        } catch (Exception e) {
            log.error("Erro ao salvar transações", e);
            return ResponseEntity.status(500).body(
                java.util.Map.of("error", "Erro ao salvar transações: " + e.getMessage())
            );
        }
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<List<ExtractTransactionResponse>> getTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("GET /transactions - UserId: {}, StartDate: {}, EndDate: {}", userId, startDate, endDate);
        try {
            List<ExtractTransactionResponse> transactions;
            
            if (startDate != null && endDate != null) {
                // Buscar por range de datas
                java.time.LocalDate start = java.time.LocalDate.parse(startDate);
                java.time.LocalDate end = java.time.LocalDate.parse(endDate);
                transactions = extractTransactionService.getTransactionsByUserAndDateRange(userId, start, end);
                log.info("Retornando {} transações para o usuário {} entre {} e {}", transactions.size(), userId, startDate, endDate);
            } else {
                // Buscar todas as transações do usuário
                transactions = extractTransactionService.getTransactionsByUser(userId);
                log.info("Retornando {} transações para o usuário {}", transactions.size(), userId);
            }
            
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            log.error("Erro ao buscar transações", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        extractTransactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/transactions")
    public ResponseEntity<Void> deleteTransactions(@RequestBody List<Long> ids) {
        log.info("DELETE /transactions - Deletando {} transações", ids.size());
        extractTransactionService.deleteTransactions(ids);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/transactions/{id}/type")
    public ResponseEntity<ExtractTransactionResponse> updateTransactionType(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> request) {
        Long expenseTypeId = request.get("expenseTypeId");
        log.info("PUT /transactions/{}/type - Atualizando tipo para {}", id, expenseTypeId);
        ExtractTransactionResponse response = extractTransactionService.updateTransactionType(id, expenseTypeId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/transactions/type")
    public ResponseEntity<List<ExtractTransactionResponse>> updateTransactionsType(
            @RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        Long expenseTypeId = ((Number) request.get("expenseTypeId")).longValue();
        log.info("PUT /transactions/type - Atualizando {} transações para tipo {}", ids.size(), expenseTypeId);
        List<ExtractTransactionResponse> responses = extractTransactionService.updateTransactionsType(ids, expenseTypeId);
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/insights")
    public ResponseEntity<ExpenseInsightsResponse> getInsights(
            @RequestParam Long userId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        log.info("GET /insights - UserId: {}, Month: {}, Year: {}", userId, month, year);
        try {
            ExpenseInsightsResponse insights = expenseInsightsService.generateInsights(userId, month, year);
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Erro ao gerar insights", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    private boolean validateTransaction(IdentifiedTransaction transaction) {
        if (transaction == null) {
            log.debug("Transação é null");
            return false;
        }
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            log.debug("Descrição inválida ou vazia");
            return false;
        }
        if (transaction.getAmount() == null || transaction.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.debug("Valor inválido: {}", transaction.getAmount());
            return false;
        }
        if (transaction.getDate() == null) {
            log.debug("Data é null");
            return false;
        }
        if (transaction.getExpenseTypeId() == null) {
            log.debug("ExpenseTypeId é null");
            return false;
        }
        // Validar se a data está em um range razoável (2020-2030)
        int year = transaction.getDate().getYear();
        if (year < 2020 || year > 2030) {
            log.debug("Ano fora do range: {}", year);
            return false;
        }
        // Validar se o mês está entre 1 e 12
        int month = transaction.getDate().getMonthValue();
        if (month < 1 || month > 12) {
            log.debug("Mês inválido: {}", month);
            return false;
        }
        return true;
    }
}

