package com.txi.service.analytics;

import java.math.BigDecimal;

public record CategorySpendDto(String categoryName, BigDecimal totalSpend) {
}
