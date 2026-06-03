package com.banco.digital.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDTO {
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;
    private String fromAccountMasked;
    private String toAccountMasked;
    private BigDecimal amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;
}
