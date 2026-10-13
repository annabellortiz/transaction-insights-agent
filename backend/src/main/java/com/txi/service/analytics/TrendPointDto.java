package com.txi.service.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPointDto(LocalDate date, BigDecimal totalSpend) {
}
