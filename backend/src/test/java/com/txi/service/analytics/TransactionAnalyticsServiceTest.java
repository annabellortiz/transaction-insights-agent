package com.txi.service.analytics;

import com.txi.db.entity.Category;
import com.txi.db.entity.TransactionEntity;
import com.txi.db.repository.CategoryRepository;
import com.txi.db.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionAnalyticsServiceTest {

    private TransactionRepository transactionRepository;
    private CategoryRepository categoryRepository;
    private TransactionAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        analyticsService = new TransactionAnalyticsService(transactionRepository, categoryRepository);
    }

    @Test
    void totalSpendByCategory_shouldAggregateAmountByCategory() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        TransactionEntity food1 = new TransactionEntity();
        food1.setCategoryId(10L);
        food1.setTxnDate(LocalDate.of(2026, 9, 2));
        food1.setAmount(new BigDecimal("45.50"));

        TransactionEntity food2 = new TransactionEntity();
        food2.setCategoryId(10L);
        food2.setTxnDate(LocalDate.of(2026, 9, 15));
        food2.setAmount(new BigDecimal("20.00"));

        TransactionEntity travel = new TransactionEntity();
        travel.setCategoryId(12L);
        travel.setTxnDate(LocalDate.of(2026, 9, 9));
        travel.setAmount(new BigDecimal("90.00"));

        when(transactionRepository.findByTxnDateBetween(start, end)).thenReturn(List.of(food1, food2, travel));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(new Category("Food")));
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(new Category("Travel")));

        List<CategorySpendDto> result = analyticsService.totalSpendByCategory(start, end);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).categoryName()).isEqualTo("Travel");
        assertThat(result.get(0).totalSpend()).isEqualByComparingTo("90.00");
        assertThat(result.get(1).categoryName()).isEqualTo("Food");
        assertThat(result.get(1).totalSpend()).isEqualByComparingTo("65.50");
    }

    @Test
    void getExtremeTransaction_shouldReturnLargestOrSmallest() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        TransactionEntity first = new TransactionEntity();
        first.setCategoryId(10L);
        first.setTxnDate(LocalDate.of(2026, 9, 4));
        first.setAmount(new BigDecimal("40.00"));
        first.setDescription("Groceries");

        TransactionEntity second = new TransactionEntity();
        second.setCategoryId(12L);
        second.setTxnDate(LocalDate.of(2026, 9, 7));
        second.setAmount(new BigDecimal("125.00"));
        second.setDescription("Flight");

        when(transactionRepository.findByTxnDateBetween(start, end)).thenReturn(List.of(first, second));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(new Category("Food")));
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(new Category("Travel")));

        TransactionSummaryDto largest = analyticsService.getExtremeTransaction(start, end, true);
        TransactionSummaryDto smallest = analyticsService.getExtremeTransaction(start, end, false);

        assertThat(largest.amount()).isEqualByComparingTo("125.00");
        assertThat(largest.categoryName()).isEqualTo("Travel");
        assertThat(largest.description()).isEqualTo("Flight");
        assertThat(smallest.amount()).isEqualByComparingTo("40.00");
        assertThat(smallest.categoryName()).isEqualTo("Food");
    }

    @Test
    void spendTrendByCategory_shouldReturnDailyTotals() {
        Long categoryId = 10L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 3);

        TransactionEntity day1 = new TransactionEntity();
        day1.setCategoryId(categoryId);
        day1.setTxnDate(LocalDate.of(2026, 9, 1));
        day1.setAmount(new BigDecimal("15.00"));

        TransactionEntity day2 = new TransactionEntity();
        day2.setCategoryId(categoryId);
        day2.setTxnDate(LocalDate.of(2026, 9, 1));
        day2.setAmount(new BigDecimal("10.00"));

        TransactionEntity day3 = new TransactionEntity();
        day3.setCategoryId(categoryId);
        day3.setTxnDate(LocalDate.of(2026, 9, 3));
        day3.setAmount(new BigDecimal("25.00"));

        when(transactionRepository.findByCategoryIdAndTxnDateBetween(categoryId, start, end)).thenReturn(List.of(day1, day2, day3));

        List<TrendPointDto> result = analyticsService.spendTrendByCategory(categoryId, start, end);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(result.get(0).totalSpend()).isEqualByComparingTo("25.00");
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2026, 9, 2));
        assertThat(result.get(1).totalSpend()).isEqualByComparingTo("0.00");
        assertThat(result.get(2).date()).isEqualTo(LocalDate.of(2026, 9, 3));
        assertThat(result.get(2).totalSpend()).isEqualByComparingTo("25.00");
    }
}
