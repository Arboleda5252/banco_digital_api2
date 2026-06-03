package com.banco.digital.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long fromAccountId;
    private String toEmail;
    private BigDecimal amount;
}
