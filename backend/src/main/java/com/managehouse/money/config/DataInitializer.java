package com.managehouse.money.config;

import com.managehouse.money.entity.ExpenseType;
import com.managehouse.money.entity.User;
import com.managehouse.money.repository.ExpenseTypeRepository;
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

    @Override
    public void run(String... args) {
        initializeUsers();
        initializeExpenseTypes();
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
        if (expenseTypeRepository.count() == 0) {
            List<String> types = Arrays.asList(
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

            for (String typeName : types) {
                ExpenseType expenseType = new ExpenseType();
                expenseType.setName(typeName);
                expenseTypeRepository.save(expenseType);
            }
        }
    }
}

