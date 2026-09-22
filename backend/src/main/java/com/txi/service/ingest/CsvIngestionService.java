package com.txi.service.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.txi.db.entity.TransactionEntity;
import com.txi.db.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CsvIngestionService {

    private final TransactionRepository txRepo;

    public CsvIngestionService(TransactionRepository txRepo) {
        this.txRepo = txRepo;
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

            for (Map<String, String> row : rows) {
                result.setTotalRows(result.getTotalRows() + 1);

                // basic validation: required columns: txn_date, amount
                String dateStr = row.get("txn_date");
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
                    LocalDate d = LocalDate.parse(dateStr);
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
                // serialize imported row as JSON using a regular ObjectMapper
                tx.setImportedRow(jsonMapper.writeValueAsString(row));

                // default account mapping: create if none (TODO: improve)
                tx.setAccountId(1L);

                txRepo.save(tx);
                result.setInsertedRows(result.getInsertedRows() + 1);
            }

        } catch (Exception e) {
            result.addError("fatal: " + e.getMessage());
        }
        return result;
    }
}
