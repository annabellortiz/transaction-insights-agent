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

    public CsvIngestionResult ingest(MultipartFile file) {
        CsvIngestionResult result = new CsvIngestionResult();
        try (InputStream in = file.getInputStream()) {
            byte[] bytes = in.readAllBytes();

            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = mapper.readerFor(new TypeReference<Map<String, String>>() {}).with(schema).readValues(new java.io.ByteArrayInputStream(bytes));
            List<Map<String, String>> rows = it.readAll();

            ObjectMapper jsonMapper = new ObjectMapper();

            for (Map<String, String> rawRow : rows) {
                Map<String, String> row = normalizeRow(rawRow);
                result.setTotalRows(result.getTotalRows() + 1);

                String dateStr = firstNonBlank(row.get("txn_date"), row.get("date"));
                String amountStr = row.get("amount");
                if (dateStr == null || dateStr.isBlank()) {
                    result.addError("row " + result.getTotalRows() + ": missing txn_date");
                    continue;
                }
                if (amountStr == null || amountStr.isBlank()) {
                    result.addError("row " + result.getTotalRows() + ": missing amount");
                    continue;
                }

                TransactionEntity tx = new TransactionEntity();
                try {
                    LocalDate d = LocalDate.parse(dateStr.trim());
                    tx.setTxnDate(d);
                } catch (Exception e) {
                    result.addError("row " + result.getTotalRows() + ": invalid date: " + dateStr);
                    continue;
                }

                try {
                    BigDecimal amt = new BigDecimal(amountStr.trim());
                    tx.setAmount(amt);
                } catch (Exception e) {
                    result.addError("row " + result.getTotalRows() + ": invalid amount: " + amountStr);
                    continue;
                }

                tx.setMerchant(row.get("merchant"));
                tx.setDescription(row.get("description"));
                tx.setCurrency(row.getOrDefault("currency", "USD"));
                tx.setImportedRow(jsonMapper.writeValueAsString(row));

                String categoryName = row.get("category");
                if (categoryName != null && !categoryName.isBlank()) {
                    Category category = categoryRepo.findByName(categoryName.trim());
                    if (category == null) {
                        category = categoryRepo.save(new Category(categoryName.trim()));
                    }
                    tx.setCategoryId(category.getId());
                }

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
            String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            String value = entry.getValue() == null ? null : entry.getValue().trim();
            normalized.put(key, value);
        }

        if (!normalized.containsKey("txn_date") && normalized.containsKey("date")) {
            normalized.put("txn_date", normalized.get("date"));
        }
        return normalized;
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
