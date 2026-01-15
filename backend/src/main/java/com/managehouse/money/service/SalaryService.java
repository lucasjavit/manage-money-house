package com.managehouse.money.service;

import com.managehouse.money.dto.AnnualSalaryCalculationResponse;
import com.managehouse.money.dto.SalaryCalculationRequest;
import com.managehouse.money.dto.SalaryCalculationResponse;
import com.managehouse.money.dto.SalaryRequest;
import com.managehouse.money.dto.SalaryResponse;
import com.managehouse.money.entity.Salary;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.managehouse.money.dto.OpenAIRequest;
import com.managehouse.money.dto.OpenAIResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryService {
    private final SalaryRepository salaryRepository;
    private final UserService userService;
    private final ConfigurationService configurationService;
    private final SalaryDeductionService salaryDeductionService;
    private final ExpenseService expenseService;
    private final SalaryConversionService salaryConversionService;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    @Value("${currency.exchange.rate.usd-to-brl:5.42}")
    private String usdToBrlRateStr; // Fallback se a IA falhar
    
    private WebClient webClient;
    private BigDecimal cachedExchangeRate;
    private long cacheTimestamp;
    private static final long CACHE_DURATION_MS = 3600000; // 1 hora
    
    private BigDecimal getUsdToBrlRate() {
        // Verificar cache (válido por 1 hora) - só usar cache se não for zero
        long currentTime = System.currentTimeMillis();
        if (cachedExchangeRate != null && 
            cachedExchangeRate.compareTo(BigDecimal.ZERO) > 0 &&
            (currentTime - cacheTimestamp) < CACHE_DURATION_MS) {
            log.info("Usando taxa de câmbio em cache: " + cachedExchangeRate);
            return cachedExchangeRate;
        }
        
        // Tentar buscar da IA
        try {
            BigDecimal rate = fetchExchangeRateFromAI();
            if (rate != null && rate.compareTo(BigDecimal.ZERO) > 0) {
                cachedExchangeRate = rate;
                cacheTimestamp = currentTime;
                log.info("Taxa de câmbio obtida da IA: " + rate);
                return rate;
            } else {
                log.warn("Taxa de câmbio retornada pela IA é inválida ou zero");
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar taxa de câmbio da IA, retornando zero: " + e.getMessage());
        }
        
        // Se a IA falhar, usar o valor de fallback configurado
        BigDecimal fallbackRate = new BigDecimal(usdToBrlRateStr);
        log.warn("Taxa de câmbio não disponível (IA falhou), usando fallback: " + fallbackRate);
        cachedExchangeRate = fallbackRate;
        cacheTimestamp = currentTime;
        return fallbackRate;
    }
    
    private BigDecimal fetchExchangeRateFromAI() {
        String apiKey = configurationService.getOpenAIKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API key não configurada, usando fallback");
            return null;
        }
        
        try {
            String prompt = "Qual é a taxa de câmbio atual de USD (Dólar Americano) para BRL (Real Brasileiro)? " +
                          "Retorne APENAS o valor numérico, sem símbolos, sem texto adicional. " +
                          "Por exemplo, se a taxa for 5.42, retorne apenas: 5.42";
            
            List<OpenAIRequest.Message> messages = new ArrayList<>();
            messages.add(new OpenAIRequest.Message("system",
                "Você é um assistente especializado em informações financeiras e taxas de câmbio. " +
                "Sua tarefa é fornecer a taxa de câmbio atual de USD para BRL."));
            messages.add(new OpenAIRequest.Message("user", prompt));
            
            OpenAIRequest openAIRequest = new OpenAIRequest(model, messages, 0.3);
            
            if (webClient == null) {
                webClient = WebClient.builder()
                    .baseUrl(apiUrl)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .build();
            }
            
            OpenAIResponse response = webClient.post()
                .bodyValue(openAIRequest)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .block();
            
            if (response != null &&
                response.getChoices() != null &&
                !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent().trim();
                
                // Extrair número da resposta (pode vir com texto)
                Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)");
                Matcher matcher = pattern.matcher(content);
                if (matcher.find()) {
                    String rateStr = matcher.group(1);
                    BigDecimal rate = new BigDecimal(rateStr);
                    log.info("Taxa de câmbio extraída da resposta da IA: " + rate);
                    return rate;
                } else {
                    log.warn("Não foi possível extrair número da resposta da IA: " + content);
                }
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Erro ao buscar taxa de câmbio da IA", e);
            return null;
        }
    }

    public SalaryResponse getSalaryByUser(Long userId) {
        return salaryRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public SalaryResponse createOrUpdateSalary(SalaryRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verifica se já existe um salário para este usuário
        // Se existir, atualiza; se não, cria novo
        Salary salary = salaryRepository
                .findByUserId(request.getUserId())
                .orElse(new Salary());

        salary.setUser(user);
        
        // Se tem fixedAmount, é salário fixo (Mariana)
        if (request.getFixedAmount() != null) {
            salary.setFixedAmount(request.getFixedAmount());
            salary.setCurrency("BRL");
            salary.setHourlyRate(null);
        }
        
        // Se tem hourlyRate, é salário variável (Lucas)
        if (request.getHourlyRate() != null) {
            salary.setHourlyRate(request.getHourlyRate());
            salary.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
            salary.setFixedAmount(null);
        }

        Salary saved = salaryRepository.save(salary);
        return toResponse(saved);
    }

    public SalaryCalculationResponse calculateVariableSalary(SalaryCalculationRequest request) {
        Salary salary = salaryRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Salary not found for user"));

        // Verificar se existe conversão salva para o mês de pagamento
        com.managehouse.money.dto.SalaryConversionResponse savedConversion = 
            salaryConversionService.getConversionByMonthAndYear(request.getUserId(), request.getMonth(), request.getYear());
        
        BigDecimal exchangeRate;
        BigDecimal totalAmount;
        BigDecimal totalAmountBRL;
        int workingDays;
        int hoursPerDay = 8;
        int totalHours;
        BigDecimal hourlyRate = salary.getHourlyRate() != null ? salary.getHourlyRate() : BigDecimal.ZERO;
        
        // Se existe conversão salva com finalAmountBRL, usar esse valor diretamente como salário recebido
        if (savedConversion != null && savedConversion.getFinalAmountBRL() != null) {
            // Usar o valor convertido final da conversão como o salário recebido
            totalAmountBRL = savedConversion.getFinalAmountBRL();
            exchangeRate = savedConversion.getVet() != null ? savedConversion.getVet() : 
                          (savedConversion.getExchangeRate() != null ? savedConversion.getExchangeRate() : getUsdToBrlRate());
            
            // Calcular dias úteis do mês ANTERIOR (mês trabalhado) para exibição
            int workMonth = request.getMonth() - 1;
            int workYear = request.getYear();
            if (workMonth < 1) {
                workMonth = 12;
                workYear = workYear - 1;
            }
            workingDays = calculateWorkingDays(workYear, workMonth);
            totalHours = workingDays * hoursPerDay;
            
            // Calcular o valor em USD baseado no valor final em BRL e a taxa de câmbio
            if (exchangeRate != null && exchangeRate.compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = totalAmountBRL
                        .divide(exchangeRate, 2, RoundingMode.HALF_UP);
            } else {
                // Fallback: usar hourlyRate * horas se disponível, senão usar amountUSD da conversão
                if (hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                    totalAmount = hourlyRate
                            .multiply(new BigDecimal(totalHours))
                            .setScale(2, RoundingMode.HALF_UP);
                } else if (savedConversion.getAmountUSD() != null) {
                    totalAmount = savedConversion.getAmountUSD();
                } else {
                    totalAmount = BigDecimal.ZERO;
                }
                exchangeRate = getUsdToBrlRate(); // Garantir que exchangeRate não seja null
            }
            
            log.info("SalaryService.calculateVariableSalary - Usando conversão salva: finalAmountBRL={}, VET={}, Valor USD calculado={}", 
                    totalAmountBRL, exchangeRate, totalAmount);
        } else {
            // Calcular dias úteis do mês ANTERIOR (mês trabalhado)
            // O mês selecionado é o mês de pagamento, mas o trabalho foi feito no mês anterior
            int workMonth = request.getMonth() - 1;
            int workYear = request.getYear();
            if (workMonth < 1) {
                workMonth = 12;
                workYear = workYear - 1;
            }
            
            workingDays = calculateWorkingDays(workYear, workMonth);
            totalHours = workingDays * hoursPerDay;
            
            // Se não tem hourlyRate, usar zero ou tentar calcular baseado na conversão se disponível
            if (hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                totalAmount = hourlyRate
                        .multiply(new BigDecimal(totalHours))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                totalAmount = BigDecimal.ZERO;
            }
            
            // Converter para BRL - usar VET da conversão se disponível, senão usar IA
            if (savedConversion != null && savedConversion.getVet() != null) {
                // Usar VET da conversão salva
                exchangeRate = savedConversion.getVet();
                totalAmountBRL = totalAmount
                        .multiply(exchangeRate)
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("SalaryService.calculateVariableSalary - Usando VET da conversão salva: VET={}, Valor final={}", exchangeRate, totalAmountBRL);
            } else {
                // Fallback: usar IA para buscar taxa de câmbio
                exchangeRate = getUsdToBrlRate();
                totalAmountBRL = totalAmount
                        .multiply(exchangeRate)
                        .setScale(2, RoundingMode.HALF_UP);
                log.info("SalaryService.calculateVariableSalary - Usando taxa de câmbio da IA: {}", exchangeRate);
            }
        }

        // Calcular descontos (boletos) do mês
        BigDecimal totalDeductions = salaryDeductionService.getTotalDeductionsByMonthAndYear(
                request.getUserId(),
                request.getMonth(),
                request.getYear()
        );

        // Calcular dívida do Lucas (se for o Lucas)
        // A dívida é baseada no mês ANTERIOR (usa primeiro, paga depois)
        BigDecimal lucasDebt = BigDecimal.ZERO;
        User user = userService.findById(request.getUserId()).orElse(null);
        if (user != null && "vyeiralucas@gmail.com".equals(user.getEmail())) {
            // Calcular dívida do Lucas para o mês ANTERIOR
            int debtMonth = request.getMonth() - 1;
            int debtYear = request.getYear();
            if (debtMonth < 1) {
                debtMonth = 12;
                debtYear = debtYear - 1;
            }
            lucasDebt = expenseService.calculateLucasDebt(debtYear, debtMonth);
            // Se lucasDebt > 0, Lucas deve para Mariana (descontar)
            // Se lucasDebt < 0, Mariana deve para Lucas (não descontar, mas pode mostrar)
            // Se lucasDebt = 0, estão quites
        }

        // Calcular salário líquido (descontar boletos e dívida se Lucas deve)
        BigDecimal netSalaryBRL = totalAmountBRL.subtract(totalDeductions);
        if (lucasDebt.compareTo(BigDecimal.ZERO) > 0) {
            // Lucas deve para Mariana, descontar do salário
            netSalaryBRL = netSalaryBRL.subtract(lucasDebt);
        }
        if (netSalaryBRL.compareTo(BigDecimal.ZERO) < 0) {
            netSalaryBRL = BigDecimal.ZERO;
        }

        log.info("SalaryService.calculateVariableSalary - Exchange Rate: {}", exchangeRate);
        log.info("SalaryService.calculateVariableSalary - Total USD: {}", totalAmount);
        log.info("SalaryService.calculateVariableSalary - Total BRL: {}", totalAmountBRL);
        log.info("SalaryService.calculateVariableSalary - Total Deductions: {}", totalDeductions);
        log.info("SalaryService.calculateVariableSalary - Lucas Debt: {}", lucasDebt);
        log.info("SalaryService.calculateVariableSalary - Net Salary BRL: {}", netSalaryBRL);

        return new SalaryCalculationResponse(
                hourlyRate,
                workingDays,
                hoursPerDay,
                new BigDecimal(totalHours),
                totalAmount,
                totalAmountBRL,
                totalDeductions,
                lucasDebt,
                netSalaryBRL,
                exchangeRate,
                salary.getCurrency() != null ? salary.getCurrency() : "USD",
                request.getMonth(),
                request.getYear()
        );
    }

    public com.managehouse.money.dto.AnnualSalaryCalculationResponse calculateAnnualSalary(Long userId, Integer year) {
        Salary salary = salaryRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Salary not found for user"));

        if (salary.getHourlyRate() == null) {
            throw new RuntimeException("User does not have hourly rate configured");
        }

        int totalWorkingDays = 0;
        int hoursPerDay = 8;
        
        // Calcular dias úteis para cada mês do ano
        for (int month = 1; month <= 12; month++) {
            totalWorkingDays += calculateWorkingDays(year, month);
        }
        
        int totalHours = totalWorkingDays * hoursPerDay;
        
        BigDecimal totalAmountUSD = salary.getHourlyRate()
                .multiply(new BigDecimal(totalHours))
                .setScale(2, RoundingMode.HALF_UP);

        // Para cálculo anual, usar média das conversões salvas ou fallback para IA
        BigDecimal exchangeRate = getUsdToBrlRate(); // Fallback padrão
        BigDecimal totalAmountBRL = totalAmountUSD
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
        
        // TODO: Implementar lógica para usar conversões salvas no cálculo anual se necessário

        // Calcular descontos (boletos) do ano inteiro
        BigDecimal totalDeductions = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            BigDecimal monthDeductions = salaryDeductionService.getTotalDeductionsByMonthAndYear(
                    userId,
                    month,
                    year
            );
            totalDeductions = totalDeductions.add(monthDeductions);
        }

        // Calcular dívida total do Lucas (se for o Lucas)
        // A dívida é baseada no mês ANTERIOR (usa primeiro, paga depois)
        BigDecimal totalLucasDebt = BigDecimal.ZERO;
        User user = userService.findById(userId).orElse(null);
        if (user != null && "vyeiralucas@gmail.com".equals(user.getEmail())) {
            // Calcular dívida do Lucas para cada mês do ano (usando mês anterior)
            for (int month = 1; month <= 12; month++) {
                int debtMonth = month - 1;
                int debtYear = year;
                if (debtMonth < 1) {
                    debtMonth = 12;
                    debtYear = debtYear - 1;
                }
                BigDecimal monthDebt = expenseService.calculateLucasDebt(debtYear, debtMonth);
                if (monthDebt.compareTo(BigDecimal.ZERO) > 0) {
                    // Lucas deve para Mariana, somar ao total
                    totalLucasDebt = totalLucasDebt.add(monthDebt);
                }
            }
        }

        // Calcular salário líquido anual (descontar boletos e dívida se Lucas deve)
        BigDecimal netSalaryBRL = totalAmountBRL.subtract(totalDeductions);
        if (totalLucasDebt.compareTo(BigDecimal.ZERO) > 0) {
            // Lucas deve para Mariana, descontar do salário
            netSalaryBRL = netSalaryBRL.subtract(totalLucasDebt);
        }
        if (netSalaryBRL.compareTo(BigDecimal.ZERO) < 0) {
            netSalaryBRL = BigDecimal.ZERO;
        }

        log.info("SalaryService.calculateAnnualSalary - Year: {}", year);
        log.info("SalaryService.calculateAnnualSalary - Total Working Days: {}", totalWorkingDays);
        log.info("SalaryService.calculateAnnualSalary - Total Hours: {}", totalHours);
        log.info("SalaryService.calculateAnnualSalary - Total USD: {}", totalAmountUSD);
        log.info("SalaryService.calculateAnnualSalary - Total BRL: {}", totalAmountBRL);
        log.info("SalaryService.calculateAnnualSalary - Total Deductions: {}", totalDeductions);
        log.info("SalaryService.calculateAnnualSalary - Total Lucas Debt: {}", totalLucasDebt);
        log.info("SalaryService.calculateAnnualSalary - Net Salary BRL: {}", netSalaryBRL);

        return new AnnualSalaryCalculationResponse(
                salary.getHourlyRate(),
                year,
                totalWorkingDays,
                hoursPerDay,
                totalHours,
                totalAmountUSD,
                totalAmountBRL,
                totalDeductions,
                totalLucasDebt,
                netSalaryBRL,
                exchangeRate,
                salary.getCurrency() != null ? salary.getCurrency() : "USD"
        );
    }

    private int calculateWorkingDays(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        
        int workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            // Conta apenas segunda a sexta (1-5)
            if (dayOfWeek.getValue() >= 1 && dayOfWeek.getValue() <= 5) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }

    @Transactional
    public void deleteSalary(Long id) {
        salaryRepository.deleteById(id);
    }

    private SalaryResponse toResponse(Salary salary) {
        return new SalaryResponse(
                salary.getId(),
                salary.getUser().getId(),
                salary.getUser().getName(),
                salary.getUser().getColor(),
                salary.getFixedAmount(),
                salary.getHourlyRate(),
                salary.getCurrency(),
                salary.getCreatedAt(),
                salary.getUpdatedAt()
        );
    }
}

