package com.txi.service.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.txi.db.entity.Category;
import com.txi.db.entity.TransactionEntity;
import com.txi.db.repository.CategoryRepository;
import com.txi.db.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class CsvIngestionService {

    private final TransactionRepository txRepo;
    private final CategoryRepository categoryRepo;

    public CsvIngestionService(TransactionRepository txRepo, CategoryRepository categoryRepo) {
        this.txRepo = txRepo;
        this.categoryRepo = categoryRepo;
    }

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M-d-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy")
    );

    public CsvIngestionResult ingest(MultipartFile file) {
        CsvIngestionResult result = new CsvIngestionResult();
        try (InputStream in = file.getInputStream()) {
            txRepo.deleteAll();

            byte[] bytes = in.readAllBytes();

            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = mapper.readerFor(new TypeReference<Map<String, String>>() {}).with(schema).readValues(new java.io.ByteArrayInputStream(bytes));
            List<Map<String, String>> rows = it.readAll();

            ObjectMapper jsonMapper = new ObjectMapper();
            Map<String, Category> categoriesByName = new HashMap<>();

            for (Map<String, String> rawRow : rows) {
                Map<String, String> row = normalizeRow(rawRow);
                result.setTotalRows(result.getTotalRows() + 1);

                String dateStr = resolveDateValue(row);
                String amountStr = resolveAmountValue(row);
                if (dateStr == null || dateStr.isBlank()) {
                    result.addError("row " + result.getTotalRows() + ": missing transaction date");
                    continue;
                }
                if (amountStr == null || amountStr.isBlank()) {
                    result.addError("row " + result.getTotalRows() + ": missing amount");
                    continue;
                }

                TransactionEntity tx = new TransactionEntity();
                try {
                    tx.setTxnDate(parseDate(dateStr));
                } catch (DateTimeParseException e) {
                    result.addError("row " + result.getTotalRows() + ": invalid date: " + dateStr);
                    continue;
                }

                try {
                    tx.setAmount(parseAmount(amountStr));
                } catch (Exception e) {
                    result.addError("row " + result.getTotalRows() + ": invalid amount: " + amountStr);
                    continue;
                }

                String merchant = firstNonBlank(
                        row.get("merchant"),
                        row.get("payee"),
                        row.get("description"),
                        row.get("name"),
                        row.get("counterparty")
                );
                tx.setMerchant(merchant);
                tx.setDescription(firstNonBlank(row.get("description"), row.get("merchant"), row.get("payee"), row.get("name")));
                tx.setCurrency(row.getOrDefault("currency", "USD"));
                tx.setImportedRow(jsonMapper.writeValueAsString(row));

                String categoryName = resolveCategoryValue(row);
                if (categoryName == null || categoryName.isBlank()) {
                    categoryName = "Uncategorized";
                }
                String normalizedCategoryName = categoryName.trim();

                Category category = categoriesByName.get(normalizedCategoryName);
                if (category == null) {
                    category = categoryRepo.findByName(normalizedCategoryName);
                    if (category == null) {
                        Category newCategory = new Category(normalizedCategoryName);
                        Category saved = categoryRepo.save(newCategory);
                        category = saved != null ? saved : newCategory;
                    }
                    categoriesByName.put(normalizedCategoryName, category);
                }
                tx.setCategoryId(category.getId());

                tx.setAccountId(1L);

                txRepo.save(tx);
                result.setInsertedRows(result.getInsertedRows() + 1);
            }

        } catch (Exception e) {
            result.addError("fatal: " + e.getMessage());
        }
        return result;
    }

    private Map<String, String> normalizeRow(Map<String, String> rawRow) {
        Map<String, String> normalized = new HashMap<>();
        for (Map.Entry<String, String> entry : rawRow.entrySet()) {
            String key = sanitizeKey(entry.getKey());
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            normalized.put(key, value);
        }

        if (!normalized.containsKey("txn_date") && normalized.containsKey("date")) {
            normalized.put("txn_date", normalized.get("date"));
        }
        return normalized;
    }

    private String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized;
    }

    private String resolveDateValue(Map<String, String> row) {
        String[] dateKeys = {"txn_date", "date", "transaction_date", "trans_date", "post_date", "posted_date", "booking_date"};
        for (String key : dateKeys) {
            String value = row.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String resolveAmountValue(Map<String, String> row) {
        String direct = firstNonBlank(
                row.get("amount"),
                row.get("transaction_amount"),
                row.get("value"),
                row.get("debit"),
                row.get("credit")
        );
        if (direct != null && !direct.isBlank()) {
            return direct;
        }

        String debit = row.get("debit");
        String credit = row.get("credit");
        if (debit != null && !debit.isBlank() && (credit == null || credit.isBlank())) {
            return debit;
        }
        if (credit != null && !credit.isBlank() && (debit == null || debit.isBlank())) {
            return credit;
        }
        return null;
    }

    private String resolveCategoryValue(Map<String, String> row) {
        return firstNonBlank(
                row.get("category"),
                row.get("category_name"),
                row.get("spending_category"),
                row.get("type")
        );
    }

    private LocalDate parseDate(String dateText) {
        String cleaned = dateText.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next common bank format.
            }
        }
        return LocalDate.parse(cleaned);
    }

    private BigDecimal parseAmount(String rawAmount) {
        String cleaned = rawAmount.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("empty amount");
        }

        String value = cleaned.replaceAll("[$,]", "");
        if (value.matches(".*[A-Za-z].*")) {
            throw new IllegalArgumentException("non-numeric amount: " + rawAmount);
        }

        BigDecimal decimal = new BigDecimal(value);
        return decimal.abs();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
