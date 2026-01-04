package com.managehouse.money.dto;

import lombok.Data;
import java.util.List;

@Data
public class SaveTransactionsRequest {
    private Long userId;
    private List<IdentifiedTransaction> transactions;
}

