package com.txi.service.analytics;

import com.txi.db.entity.TransactionEntity;
import com.txi.db.repository.CategoryRepository;
import com.txi.db.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TransactionAnalyticsService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionAnalyticsService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<CategorySpendDto> totalSpendByCategory(LocalDate start, LocalDate end) {
        Map<Long, BigDecimal> totalsByCategory = new HashMap<>();

        for (TransactionEntity tx : transactionRepository.findByTxnDateBetween(start, end)) {
            if (tx.getCategoryId() == null || tx.getAmount() == null) {
                continue;
            }
            totalsByCategory.merge(tx.getCategoryId(), tx.getAmount(), BigDecimal::add);
        }

        return totalsByCategory.entrySet().stream()
                .map(entry -> new CategorySpendDto(resolveCategoryName(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparing(CategorySpendDto::totalSpend).reversed())
                .toList();
    }

    public TransactionSummaryDto getExtremeTransaction(LocalDate start, LocalDate end, boolean largest) {
        List<TransactionEntity> transactions = transactionRepository.findByTxnDateBetween(start, end).stream()
                .filter(tx -> tx.getAmount() != null)
                .toList();

        if (transactions.isEmpty()) {
            return null;
        }

        Comparator<TransactionEntity> comparator = Comparator.comparing(TransactionEntity::getAmount);
        TransactionEntity chosen = largest
                ? transactions.stream().max(comparator).orElse(null)
                : transactions.stream().min(comparator).orElse(null);

        if (chosen == null) {
            return null;
        }

        return new TransactionSummaryDto(
                chosen.getTxnDate(),
                resolveCategoryName(chosen.getCategoryId()),
                chosen.getAmount(),
                chosen.getMerchant(),
                chosen.getDescription()
        );
    }

    public List<TrendPointDto> spendTrendByCategory(Long categoryId, LocalDate start, LocalDate end) {
        Map<LocalDate, BigDecimal> totalsByDate = new TreeMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            totalsByDate.put(cursor, BigDecimal.ZERO);
            cursor = cursor.plusDays(1);
        }

        for (TransactionEntity tx : transactionRepository.findByCategoryIdAndTxnDateBetween(categoryId, start, end)) {
            if (tx.getTxnDate() == null || tx.getAmount() == null) {
                continue;
            }
            totalsByDate.merge(tx.getTxnDate(), tx.getAmount(), BigDecimal::add);
        }

        return totalsByDate.entrySet().stream()
                .map(entry -> new TrendPointDto(entry.getKey(), entry.getValue()))
                .toList();
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return "Uncategorized";
        }
        return categoryRepository.findById(categoryId)
                .map(category -> category.getName())
                .orElse("Unknown");
    }
}
