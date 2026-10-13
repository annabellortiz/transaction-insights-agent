package com.txi.api;

import com.txi.service.analytics.CategorySpendDto;
import com.txi.service.analytics.TransactionAnalyticsService;
import com.txi.service.analytics.TransactionSummaryDto;
import com.txi.service.analytics.TrendPointDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class TransactionAnalyticsController {

    private final TransactionAnalyticsService transactionAnalyticsService;

    public TransactionAnalyticsController(TransactionAnalyticsService transactionAnalyticsService) {
        this.transactionAnalyticsService = transactionAnalyticsService;
    }

    @GetMapping("/spend-by-category")
    public List<CategorySpendDto> spendByCategory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return transactionAnalyticsService.totalSpendByCategory(start, end);
    }

    @GetMapping("/largest-transaction")
    public TransactionSummaryDto largestTransaction(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return transactionAnalyticsService.getExtremeTransaction(start, end, true);
    }

    @GetMapping("/smallest-transaction")
    public TransactionSummaryDto smallestTransaction(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return transactionAnalyticsService.getExtremeTransaction(start, end, false);
    }

    @GetMapping("/spend-trend")
    public List<TrendPointDto> spendTrend(
            @RequestParam Long categoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return transactionAnalyticsService.spendTrendByCategory(categoryId, start, end);
    }
}
