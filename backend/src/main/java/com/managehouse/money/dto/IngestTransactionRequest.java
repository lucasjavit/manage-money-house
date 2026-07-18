package com.managehouse.money.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Transação enviada pelo app Android a partir de uma notificação de banco.
 */
@Data
public class IngestTransactionRequest {
    // Dono da transação (o app envia o id do Lucas).
    private Long userId;
    // Hash gerado no celular (packageName + texto + minuto) para idempotência.
    private String externalId;
    private String description;
    private BigDecimal amount;
    // Epoch millis da notificação; define o mês/ano do lançamento.
    private Long timestamp;
    private String packageName;
    // "personal" (gasto do Lucas) ou "house" (planilha da casa).
    private String destination;
    // Obrigatório quando destination = "house": o tipo escolhido no celular.
    private Long expenseTypeId;
    // Texto cru da notificação (auditoria / banco desconhecido).
    private String rawText;
}
