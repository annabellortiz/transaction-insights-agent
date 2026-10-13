package com.txi.service.tools;

import com.txi.service.analytics.CategorySpendDto;
import com.txi.service.analytics.TransactionAnalyticsService;
import com.txi.service.analytics.TransactionSummaryDto;
import com.txi.service.analytics.TrendPointDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionInsightsToolService {

    private final TransactionAnalyticsService analyticsService;

    public TransactionInsightsToolService(TransactionAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public List<CategorySpendDto> sumByCategory(DateRangeRequest request) {
        return analyticsService.totalSpendByCategory(request.start(), request.end());
    }

    public TransactionSummaryDto largestTransaction(DateRangeRequest request) {
        return analyticsService.getExtremeTransaction(request.start(), request.end(), true);
    }

    public TransactionSummaryDto smallestTransaction(DateRangeRequest request) {
        return analyticsService.getExtremeTransaction(request.start(), request.end(), false);
    }

    public List<TrendPointDto> trendByCategory(TrendByCategoryRequest request) {
        return analyticsService.spendTrendByCategory(request.categoryId(), request.start(), request.end());
    }
}
