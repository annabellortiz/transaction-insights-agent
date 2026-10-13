package com.txi.service.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionSummaryDto(
        LocalDate txnDate,
        String categoryName,
        BigDecimal amount,
        String merchant,
        String description
) {
}
