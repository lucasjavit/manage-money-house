package com.managehouse.money.service;

import com.managehouse.money.dto.ExpenseRequest;
import com.managehouse.money.dto.ExpenseResponse;
import com.managehouse.money.dto.IngestResponse;
import com.managehouse.money.dto.IngestTransactionRequest;
import com.managehouse.money.entity.BankTransaction;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.BankTransactionRepository;
import com.managehouse.money.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestService {

    public static final String DEST_HOUSE = "house";
    public static final String DEST_PERSONAL = "personal";
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final BankTransactionRepository bankTransactionRepository;
    private final UserRepository userRepository;
    private final ExpenseService expenseService;
    private final NotificationExtractionService extractionService;

    @Transactional
    public IngestResponse ingest(IngestTransactionRequest request) {
        boolean isHouse = DEST_HOUSE.equalsIgnoreCase(request.getDestination());
        validate(request, isHouse);

        // Dedupe ANTES de chamar a IA: reenvios não gastam Claude.
        if (bankTransactionRepository.existsByExternalId(request.getExternalId())) {
            log.info("Ingest ignorado (duplicado): externalId={}", request.getExternalId());
            return new IngestResponse("duplicate", null, null, null, null, false);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + request.getUserId()));

        LocalDate date = resolveDate(request.getTimestamp());

        // O app pode mandar o valor/tipo já editados; se não, a IA extrai do texto cru.
        BigDecimal amount = request.getAmount();
        String description = request.getDescription();
        Long expenseTypeId = request.getExpenseTypeId();

        if (amount == null || (isHouse && expenseTypeId == null) || isBlank(description)) {
            NotificationExtractionService.ExtractionResult ex =
                    extractionService.extract(request.getRawText(), isHouse);
            if (amount == null) {
                amount = ex.amount();
            }
            if (isBlank(description) && ex.description() != null) {
                description = ex.description();
            }
            if (isHouse && expenseTypeId == null) {
                expenseTypeId = ex.suggestedExpenseTypeId();
            }
        }

        // Sem valor => não dá para lançar. Grava para revisão sem perder o texto.
        boolean needsReview = amount == null;

        BankTransaction tx = new BankTransaction();
        tx.setUser(user);
        tx.setExternalId(request.getExternalId());
        tx.setDescription(description != null ? description : fallbackDescription(request));
        tx.setAmount(amount);
        tx.setTransactionDate(date);
        tx.setSourcePackage(request.getPackageName());
        tx.setRawText(request.getRawText());
        tx.setNeedsReview(needsReview);

        try {
            tx = bankTransactionRepository.save(tx);
        } catch (DataIntegrityViolationException e) {
            log.info("Ingest concorrente (duplicado no índice): externalId={}", request.getExternalId());
            return new IngestResponse("duplicate", null, null, null, null, false);
        }

        if (needsReview) {
            return new IngestResponse("needs_review", tx.getId(), null, null, expenseTypeId, true);
        }

        Long createdExpenseId = null;
        if (isHouse && expenseTypeId != null) {
            createdExpenseId = postToHouseSheet(user, expenseTypeId, amount, description, date);
        }

        return new IngestResponse("created", tx.getId(), createdExpenseId, amount, expenseTypeId, false);
    }

    /** Destino "casa": reusa o fluxo da planilha compartilhada, no mês/ano da notificação. */
    private Long postToHouseSheet(User user, Long expenseTypeId, BigDecimal amount, String description, LocalDate date) {
        ExpenseRequest expenseRequest = new ExpenseRequest();
        expenseRequest.setId(null);
        expenseRequest.setUserId(user.getId());
        expenseRequest.setExpenseTypeId(expenseTypeId);
        expenseRequest.setAmount(amount);
        expenseRequest.setMonth(date.getMonthValue());
        expenseRequest.setYear(date.getYear());
        expenseRequest.setDescription(description);

        ExpenseResponse expense = expenseService.createOrUpdateExpense(expenseRequest);
        return expense.getId();
    }

    private void validate(IngestTransactionRequest r, boolean isHouse) {
        if (isBlank(r.getExternalId())) {
            throw new IllegalArgumentException("externalId é obrigatório");
        }
        if (r.getUserId() == null) {
            throw new IllegalArgumentException("userId é obrigatório");
        }
        String dest = r.getDestination();
        if (!DEST_HOUSE.equalsIgnoreCase(dest) && !DEST_PERSONAL.equalsIgnoreCase(dest)) {
            throw new IllegalArgumentException("destination deve ser 'house' ou 'personal'");
        }
        // Com a IA extraindo, amount/description/expenseTypeId podem vir vazios — mas então
        // precisamos do texto para extrair. Sem valor E sem texto não há o que fazer.
        if (r.getAmount() == null && isBlank(r.getRawText())) {
            throw new IllegalArgumentException("informe amount ou rawText (texto da notificação)");
        }
    }

    private String fallbackDescription(IngestTransactionRequest r) {
        if (!isBlank(r.getRawText())) {
            String t = r.getRawText().trim();
            return t.length() > 500 ? t.substring(0, 500) : t;
        }
        return "Transação";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Data usada para o lançamento. Regra do usuário: tudo que vem do celular é registrado no
     * MÊS SEGUINTE ao da transação (mês da transação + 1). plusMonths trata virada de ano e
     * dias inexistentes (ex: 31/jan -> 28/fev).
     */
    private LocalDate resolveDate(Long timestamp) {
        LocalDate base = (timestamp == null)
                ? LocalDate.now(ZONE)
                : Instant.ofEpochMilli(timestamp).atZone(ZONE).toLocalDate();
        return base.plusMonths(1);
    }
}
