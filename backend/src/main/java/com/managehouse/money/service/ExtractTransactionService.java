package com.managehouse.money.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.managehouse.money.dto.ExtractTransactionResponse;
import com.managehouse.money.dto.IdentifiedTransaction;
import com.managehouse.money.entity.ExtractExpenseType;
import com.managehouse.money.entity.ExtractTransaction;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExtractExpenseTypeRepository;
import com.managehouse.money.repository.ExtractTransactionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtractTransactionService {
    private final ExtractTransactionRepository extractTransactionRepository;
    private final ExtractExpenseTypeRepository extractExpenseTypeRepository;
    private final UserService userService;
    private final ExtractExpenseTypeService extractExpenseTypeService;
    private final ExtractService extractService;

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
        
        for (IdentifiedTransaction originalTransaction : transactions) {
            try {
                log.debug("Processando transação: {}", originalTransaction.getDescription());
                log.debug("  - ExpenseTypeId: {}", originalTransaction.getExpenseTypeId());
                log.debug("  - ExpenseTypeName: {}", originalTransaction.getExpenseTypeName());
                log.debug("  - Confidence: {}", originalTransaction.getConfidence());
                log.debug("  - Amount: {}", originalTransaction.getAmount());
                log.debug("  - Date: {}", originalTransaction.getDate());
                
                // Melhorar identificação do tipo usando IA para pesquisar/analisar o nome da transação
                // Sempre tentar melhorar, especialmente quando confiança for baixa ou tipo for "Outros"
                IdentifiedTransaction transactionToUse = originalTransaction;
                boolean shouldImprove = (originalTransaction.getConfidence() != null && 
                        ("low".equalsIgnoreCase(originalTransaction.getConfidence()) || 
                         "medium".equalsIgnoreCase(originalTransaction.getConfidence()))) ||
                        (originalTransaction.getExpenseTypeName() != null && 
                         "Outros".equalsIgnoreCase(originalTransaction.getExpenseTypeName()));
                
                if (shouldImprove) {
                    log.info("Melhorando identificação do tipo usando IA para transação: {}", originalTransaction.getDescription());
                    try {
                        IdentifiedTransaction improved = extractService.improveTransactionTypeWithAI(originalTransaction);
                        if (improved != null && improved.getExpenseTypeId() != null && 
                            improved.getExpenseTypeName() != null) {
                            String oldType = originalTransaction.getExpenseTypeName();
                            transactionToUse = improved;
                            log.info("Tipo melhorado via IA: '{}' -> '{}' ({} -> {})", 
                                originalTransaction.getDescription(),
                                oldType, 
                                improved.getExpenseTypeName(),
                                improved.getConfidence());
                        }
                    } catch (Exception e) {
                        log.warn("Erro ao melhorar tipo com IA, usando tipo original: {}", e.getMessage());
                    }
                }
                
                final IdentifiedTransaction finalTransaction = transactionToUse;

                // Se ainda não tem tipo, usar "Outros" como fallback
                final Long expenseTypeId;
                if (finalTransaction.getExpenseTypeId() == null) {
                    log.warn("Transação sem expenseTypeId após tentativas de melhoria. Usando 'Outros' como fallback: {}", finalTransaction.getDescription());
                    ExtractExpenseType outrosType = extractExpenseTypeService.findByName("Outros")
                            .orElseGet(() -> {
                                log.warn("Tipo 'Outros' não encontrado! Criando...");
                                ExtractExpenseType newOutros = new ExtractExpenseType();
                                newOutros.setName("Outros");
                                return extractExpenseTypeRepository.save(newOutros);
                            });
                    expenseTypeId = outrosType.getId();
                    log.info("Usando 'Outros' (ID: {}) para transação: {}", expenseTypeId, finalTransaction.getDescription());
                } else {
                    expenseTypeId = finalTransaction.getExpenseTypeId();
                }

                ExtractExpenseType extractExpenseType = extractExpenseTypeService.findById(expenseTypeId)
                        .orElseThrow(() -> {
                            log.error("ERRO: ExtractExpenseType não encontrado com ID: {}", expenseTypeId);
                            return new RuntimeException("Extract expense type not found: " + expenseTypeId);
                        });
                
                log.debug("  - ExtractExpenseType encontrado: {} (ID: {})", extractExpenseType.getName(), extractExpenseType.getId());

                ExtractTransaction extractTransaction = new ExtractTransaction();
                extractTransaction.setUser(user);
                extractTransaction.setExtractExpenseType(extractExpenseType);
                extractTransaction.setDescription(finalTransaction.getDescription());
                extractTransaction.setAmount(finalTransaction.getAmount());
                extractTransaction.setTransactionDate(finalTransaction.getDate());

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
                log.error("ERRO ao salvar transação: {}", originalTransaction.getDescription(), e);

                // Transformar erro técnico em mensagem amigável
                String friendlyMessage;
                String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

                if (errorMessage.contains("expense_type_id") || errorMessage.contains("not-null constraint")) {
                    friendlyMessage = "A transação '" + originalTransaction.getDescription() + "' não pôde ser categorizada automaticamente. Por favor, revise e tente novamente.";
                } else if (errorMessage.contains("duplicate") || errorMessage.contains("unique")) {
                    friendlyMessage = "A transação '" + originalTransaction.getDescription() + "' já foi salva anteriormente.";
                } else if (errorMessage.contains("foreign key") || errorMessage.contains("violates")) {
                    friendlyMessage = "Dados inválidos para a transação '" + originalTransaction.getDescription() + "'. Verifique as informações e tente novamente.";
                } else {
                    friendlyMessage = "Erro ao processar a transação '" + originalTransaction.getDescription() + "'. Verifique os dados e tente novamente.";
                }

                throw new RuntimeException(friendlyMessage, e);
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
    
    @Transactional
    public void deleteTransactions(List<Long> ids) {
        log.info("Deletando {} transações de extrato", ids.size());
        extractTransactionRepository.deleteAllById(ids);
        log.info("{} transações deletadas com sucesso", ids.size());
    }
    
    @Transactional
    public ExtractTransactionResponse updateTransactionType(Long id, Long expenseTypeId) {
        log.info("Atualizando tipo da transação {} para expenseTypeId {}", id, expenseTypeId);
        
        ExtractTransaction transaction = extractTransactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ERRO: Transação não encontrada com ID: {}", id);
                    return new RuntimeException("Transaction not found: " + id);
                });
        
        ExtractExpenseType extractExpenseType = extractExpenseTypeService.findById(expenseTypeId)
                .orElseThrow(() -> {
                    log.error("ERRO: ExtractExpenseType não encontrado com ID: {}", expenseTypeId);
                    return new RuntimeException("Extract expense type not found: " + expenseTypeId);
                });
        
        transaction.setExtractExpenseType(extractExpenseType);
        ExtractTransaction saved = extractTransactionRepository.save(transaction);
        
        log.info("Tipo da transação {} atualizado para {}", id, extractExpenseType.getName());
        return toResponse(saved);
    }
    
    @Transactional
    public List<ExtractTransactionResponse> updateTransactionsType(List<Long> ids, Long expenseTypeId) {
        log.info("Atualizando tipo de {} transações para expenseTypeId {}", ids.size(), expenseTypeId);
        
        ExtractExpenseType extractExpenseType = extractExpenseTypeService.findById(expenseTypeId)
                .orElseThrow(() -> {
                    log.error("ERRO: ExtractExpenseType não encontrado com ID: {}", expenseTypeId);
                    return new RuntimeException("Extract expense type not found: " + expenseTypeId);
                });
        
        List<ExtractTransaction> transactions = extractTransactionRepository.findAllById(ids);
        
        if (transactions.size() != ids.size()) {
            log.warn("Algumas transações não foram encontradas. Esperado: {}, Encontrado: {}", ids.size(), transactions.size());
        }
        
        transactions.forEach(transaction -> {
            transaction.setExtractExpenseType(extractExpenseType);
        });
        
        List<ExtractTransaction> saved = extractTransactionRepository.saveAll(transactions);
        
        log.info("{} transações atualizadas com sucesso", saved.size());
        return saved.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ExtractTransactionResponse toResponse(ExtractTransaction transaction) {
        return new ExtractTransactionResponse(
                transaction.getId(),
                transaction.getUser().getId(),
                transaction.getUser().getName(),
                transaction.getUser().getColor(),
                transaction.getExtractExpenseType().getId(),
                transaction.getExtractExpenseType().getName(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt()
        );
    }
}

