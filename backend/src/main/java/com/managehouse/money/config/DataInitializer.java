package com.managehouse.money.config;

import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.entity.ExtractExpenseType;
import com.managehouse.money.entity.ExtractTransaction;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExpenseTypeRepository;
import com.managehouse.money.repository.ExtractExpenseTypeRepository;
import com.managehouse.money.repository.ExtractTransactionRepository;
import com.managehouse.money.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final ExpenseTypeRepository expenseTypeRepository;
    private final ExtractExpenseTypeRepository extractExpenseTypeRepository;
    private final ExtractTransactionRepository extractTransactionRepository;

    @Override
    public void run(String... args) {
        initializeUsers();
        initializeExpenseTypes();
        initializeExtractExpenseTypes();
        migrateExtractTransactions();
    }

    private void initializeUsers() {
        if (userRepository.count() == 0) {
            User lucas = new User();
            lucas.setEmail("vyeiralucas@gmail.com");
            lucas.setName("Lucas");
            lucas.setColor("blue");
            userRepository.save(lucas);

            User mariana = new User();
            mariana.setEmail("marii_borges@hotmail.com");
            mariana.setName("Mariana");
            mariana.setColor("pink");
            userRepository.save(mariana);
        }
    }

    private void initializeExpenseTypes() {
        // Tipos para Planilha de Despesas (tipos originais)
        List<String> expenseSheetTypes = Arrays.asList(
                "Aluguel",
                "Condomínio",
                "Luz",
                "Água",
                "Gás",
                "IPTU",
                "Internet",
                "Mercado",
                "Marmitas",
                "Saladas",
                "Diarista",
                "Viagem",
                "Carro",
                "Outros"
        );

        // Buscar todos os tipos existentes no banco
        List<ExpenseType> existingTypes = expenseTypeRepository.findAll();
        
        // Verificar quais tipos requeridos já existem e criar os que faltam
        for (String requiredTypeName : expenseSheetTypes) {
            boolean exists = existingTypes.stream()
                    .anyMatch(et -> et.getName().equals(requiredTypeName));
            
            if (!exists) {
                // Criar o tipo que não existe
                ExpenseType expenseType = new ExpenseType();
                expenseType.setName(requiredTypeName);
                expenseTypeRepository.save(expenseType);
                System.out.println("Tipo de despesa (Planilha) criado: " + requiredTypeName);
            }
        }
        
        System.out.println("Inicialização de tipos de despesa (Planilha) concluída. Total: " + expenseTypeRepository.count());
    }
    
    private void initializeExtractExpenseTypes() {
        // Tipos para Cartão de Crédito (novos tipos)
        List<String> extractTypes = Arrays.asList(
                "Alimentação",
                "Moradia",
                "Saúde",
                "Automotivo",
                "Transporte",
                "Educação",
                "Lazer",
                "Vestuário",
                "Tecnologia",
                "Serviços",
                "Compras",
                "Financeiro",
                "Pets",
                "Cuidados Pessoais",
                "Delivery",
                "Outros"
        );

        // Buscar todos os tipos existentes no banco
        List<ExtractExpenseType> existingTypes = extractExpenseTypeRepository.findAll();
        
        // Verificar quais tipos requeridos já existem e criar os que faltam
        for (String requiredTypeName : extractTypes) {
            boolean exists = existingTypes.stream()
                    .anyMatch(et -> et.getName().equals(requiredTypeName));
            
            if (!exists) {
                // Criar o tipo que não existe
                ExtractExpenseType extractExpenseType = new ExtractExpenseType();
                extractExpenseType.setName(requiredTypeName);
                extractExpenseTypeRepository.save(extractExpenseType);
                System.out.println("Tipo de despesa (Cartão de Crédito) criado: " + requiredTypeName);
            }
        }
        
        System.out.println("Inicialização de tipos de despesa (Cartão de Crédito) concluída. Total: " + extractExpenseTypeRepository.count());
    }
    
    private void migrateExtractTransactions() {
        // Migrar transações existentes que não têm extractExpenseType
        List<ExtractTransaction> transactionsWithoutType = extractTransactionRepository.findAll().stream()
                .filter(t -> t.getExtractExpenseType() == null)
                .collect(java.util.stream.Collectors.toList());
        
        if (!transactionsWithoutType.isEmpty()) {
            System.out.println("Migrando " + transactionsWithoutType.size() + " transações sem tipo...");
            
            // Buscar o tipo "Outros" como padrão
            ExtractExpenseType outrosType = extractExpenseTypeRepository.findByName("Outros")
                    .orElse(null);
            
            if (outrosType == null) {
                System.out.println("AVISO: Tipo 'Outros' não encontrado! Criando...");
                outrosType = new ExtractExpenseType();
                outrosType.setName("Outros");
                outrosType = extractExpenseTypeRepository.save(outrosType);
            }
            
            // Atualizar todas as transações sem tipo
            for (ExtractTransaction transaction : transactionsWithoutType) {
                transaction.setExtractExpenseType(outrosType);
                extractTransactionRepository.save(transaction);
            }
            
            System.out.println("Migração concluída: " + transactionsWithoutType.size() + " transações atualizadas com tipo 'Outros'");
        }
    }
}

