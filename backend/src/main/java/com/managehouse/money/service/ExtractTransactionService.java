package com.managehouse.money.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.managehouse.money.dto.ExtractTransactionResponse;
import com.managehouse.money.dto.IdentifiedTransaction;
import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.entity.ExtractTransaction;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExtractTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractTransactionService {
    private final ExtractTransactionRepository extractTransactionRepository;
    private final UserService userService;
    private final ExpenseTypeService expenseTypeService;

    @Transactional
    public List<ExtractTransactionResponse> saveTransactions(Long userId, List<IdentifiedTransaction> transactions) {
        log.info("ExtractTransactionService.saveTransactions - UserId: {}, Quantidade: {}", userId, transactions.size());
        
        User user = userService.findById(userId)
                .orElseThrow(() -> {
                    log.error("ERRO: User não encontrado com ID: {}", userId);
                    return new RuntimeException("User not found: " + userId);
                });
        
        log.info("User encontrado: {} (ID: {})", user.getName(), user.getId());

        List<ExtractTransactionResponse> savedResponses = new java.util.ArrayList<>();
        
        for (IdentifiedTransaction transaction : transactions) {
            try {
                log.debug("Processando transação: {}", transaction.getDescription());
                log.debug("  - ExpenseTypeId: {}", transaction.getExpenseTypeId());
                log.debug("  - Amount: {}", transaction.getAmount());
                log.debug("  - Date: {}", transaction.getDate());
                
                ExpenseType expenseType = expenseTypeService.findById(transaction.getExpenseTypeId())
                        .orElseThrow(() -> {
                            log.error("ERRO: ExpenseType não encontrado com ID: {}", transaction.getExpenseTypeId());
                            return new RuntimeException("Expense type not found: " + transaction.getExpenseTypeId());
                        });
                
                log.debug("  - ExpenseType encontrado: {} (ID: {})", expenseType.getName(), expenseType.getId());

                ExtractTransaction extractTransaction = new ExtractTransaction();
                extractTransaction.setUser(user);
                extractTransaction.setExpenseType(expenseType);
                extractTransaction.setDescription(transaction.getDescription());
                extractTransaction.setAmount(transaction.getAmount());
                extractTransaction.setTransactionDate(transaction.getDate());

                log.debug("  - Salvando no banco...");
                ExtractTransaction saved = extractTransactionRepository.save(extractTransaction);
                log.debug("  - ✓ Transação salva com ID: {}", saved.getId());
                
                // Verificar se realmente foi salvo
                ExtractTransaction verify = extractTransactionRepository.findById(saved.getId()).orElse(null);
                if (verify == null) {
                    log.error("  - ✗ ERRO: Transação não encontrada após salvar! ID: {}", saved.getId());
                } else {
                    log.debug("  - ✓ Verificação: Transação encontrada no banco");
                }
                
                savedResponses.add(toResponse(saved));
            } catch (Exception e) {
                log.error("ERRO ao salvar transação: {}", transaction.getDescription(), e);
                throw new RuntimeException("Erro ao salvar transação: " + transaction.getDescription() + " - " + e.getMessage(), e);
            }
        }
        
        log.info("Total de transações salvas com sucesso: {}", savedResponses.size());
        return savedResponses;
    }

    public List<ExtractTransactionResponse> getTransactionsByUser(Long userId) {
        log.info("ExtractTransactionService.getTransactionsByUser - UserId: {}", userId);
        
        // Debug: Verificar todas as transações no banco
        List<ExtractTransaction> allTransactions = extractTransactionRepository.findAll();
        log.debug("Total de transações no banco (todas): {}", allTransactions.size());
        if (!allTransactions.isEmpty()) {
            log.debug("Primeira transação encontrada - UserId: {}, Descrição: {}", allTransactions.get(0).getUser().getId(), allTransactions.get(0).getDescription());
        }
        
        List<ExtractTransaction> transactions = extractTransactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
        log.info("Transações encontradas no banco para userId {}: {}", userId, transactions.size());
        
        if (transactions.isEmpty() && !allTransactions.isEmpty()) {
            log.warn("AVISO: Há transações no banco, mas nenhuma para o userId {}", userId);
            log.debug("UserIds das transações existentes:");
            allTransactions.forEach(t -> log.debug("  - Transação ID: {}, UserId: {}", t.getId(), t.getUser().getId()));
        }
        
        List<ExtractTransactionResponse> responses = transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.debug("Transações convertidas para response: {}", responses.size());
        return responses;
    }

    public List<ExtractTransactionResponse> getTransactionsByUserAndDateRange(
            Long userId, 
            java.time.LocalDate startDate, 
            java.time.LocalDate endDate) {
        log.info("ExtractTransactionService.getTransactionsByUserAndDateRange - UserId: {}", userId);
        log.debug("  - StartDate: {}", startDate);
        log.debug("  - EndDate: {}", endDate);
        
        List<ExtractTransaction> transactions = extractTransactionRepository.findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                userId, startDate, endDate);
        
        log.info("  - Transações encontradas no range: {}", transactions.size());
        if (!transactions.isEmpty()) {
            log.debug("  - Primeira transação: {}", transactions.get(0).getTransactionDate());
            log.debug("  - Última transação: {}", transactions.get(transactions.size() - 1).getTransactionDate());
        }
        
        return transactions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTransaction(Long id) {
        log.info("Deletando transação de extrato com ID: {}", id);
        extractTransactionRepository.deleteById(id);
        log.info("Transação de extrato com ID {} deletada com sucesso", id);
    }

    private ExtractTransactionResponse toResponse(ExtractTransaction transaction) {
        return new ExtractTransactionResponse(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getUser().getName(),
                transaction.getUser().getColor(),
                transaction.getExpenseType().getId(),
                transaction.getExpenseType().getName(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }
}

