package com.txi.service.tools;

import com.txi.service.analytics.CategorySpendDto;
import com.txi.service.analytics.TransactionAnalyticsService;
import com.txi.service.analytics.TransactionSummaryDto;
import com.txi.service.analytics.TrendPointDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionInsightsToolServiceTest {

    private final TransactionAnalyticsService analyticsService = mock(TransactionAnalyticsService.class);
    private final TransactionInsightsToolService toolService = new TransactionInsightsToolService(analyticsService);

    @Test
    public void sumByCategory_delegatesToAnalyticsService() {
        DateRangeRequest request = new DateRangeRequest(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        List<CategorySpendDto> expected = List.of(new CategorySpendDto("Groceries", new BigDecimal("100.00")));

        when(analyticsService.totalSpendByCategory(request.start(), request.end())).thenReturn(expected);

        assertThat(toolService.sumByCategory(request)).isEqualTo(expected);
    }

    @Test
    public void largestTransaction_delegatesToAnalyticsService() {
        DateRangeRequest request = new DateRangeRequest(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        TransactionSummaryDto expected = new TransactionSummaryDto(
                LocalDate.of(2026, 4, 30),
                "Housing",
                new BigDecimal("1450.00"),
                null,
                "Rent Payment"
        );

        when(analyticsService.getExtremeTransaction(request.start(), request.end(), true)).thenReturn(expected);

        assertThat(toolService.largestTransaction(request)).isEqualTo(expected);
    }

    @Test
    public void trendByCategory_delegatesToAnalyticsService() {
        TrendByCategoryRequest request = new TrendByCategoryRequest(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        List<TrendPointDto> expected = List.of(new TrendPointDto(LocalDate.of(2026, 4, 3), new BigDecimal("54.32")));

        when(analyticsService.spendTrendByCategory(request.categoryId(), request.start(), request.end())).thenReturn(expected);

        assertThat(toolService.trendByCategory(request)).isEqualTo(expected);
    }
}
