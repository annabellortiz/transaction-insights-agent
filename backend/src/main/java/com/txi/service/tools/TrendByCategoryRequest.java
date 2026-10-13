package com.txi.service.tools;

import java.time.LocalDate;

public record TrendByCategoryRequest(Long categoryId, LocalDate start, LocalDate end) {
}
