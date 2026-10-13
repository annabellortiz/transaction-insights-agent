package com.txi.service.ingest;

import com.txi.db.entity.Category;
import com.txi.db.entity.TransactionEntity;
import com.txi.db.repository.CategoryRepository;
import com.txi.db.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class CsvIngestionServiceTest {

    private TransactionRepository txRepo;
    private CategoryRepository categoryRepo;
    private CsvIngestionService ingestionService;

    @BeforeEach
    public void setUp() {
        txRepo = mock(TransactionRepository.class);
        categoryRepo = mock(CategoryRepository.class);
        ingestionService = new CsvIngestionService(txRepo, categoryRepo);
    }

    @Test
    public void ingest_validCsv_insertsRows() throws Exception {
        String csv = "txn_date,amount,merchant,description\n" +
                "2026-09-01,12.34,Cafe,latte\n" +
                "2026-09-02,45.00,Store,groceries\n";

        MockMultipartFile file = new MockMultipartFile("file", "sample.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(txRepo.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getInsertedRows()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(txRepo, times(2)).save(captor.capture());
        List<TransactionEntity> saved = captor.getAllValues();
        assertThat(saved.get(0).getMerchant()).isEqualTo("Cafe");
        assertThat(saved.get(1).getMerchant()).isEqualTo("Store");
    }

    @Test
    public void ingest_sampleCsv_withDateAndCategoryHeaders_insertsRows() throws Exception {
        String csv = "date,description,category,amount,account\n" +
                "2026-04-03,Trader Joe's,Groceries,54.32,Checking\n" +
                "2026-04-05,Shell Gas Station,Transportation,42.10,Checking\n";

        MockMultipartFile file = new MockMultipartFile("file", "sample.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        Category groceries = new Category("Groceries");
        groceries.setId(10L);
        Category transportation = new Category("Transportation");
        transportation.setId(20L);

        when(categoryRepo.findByName("Groceries")).thenReturn(groceries);
        when(categoryRepo.findByName("Transportation")).thenReturn(transportation);
        when(txRepo.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getInsertedRows()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();

        ArgumentCaptor<TransactionEntity> captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(txRepo, times(2)).save(captor.capture());
        List<TransactionEntity> saved = captor.getAllValues();
        assertThat(saved.get(0).getTxnDate()).isEqualTo(java.time.LocalDate.of(2026, 4, 3));
        assertThat(saved.get(0).getCategoryId()).isEqualTo(10L);
        assertThat(saved.get(1).getCategoryId()).isEqualTo(20L);
    }

    @Test
    public void ingest_invalidRows_collectsErrors() throws Exception {
        String csv = "txn_date,amount,merchant\n" +
                ",12.00,NoDate\n" +
                "2026-09-01,not_a_number,BadAmount\n";

        MockMultipartFile file = new MockMultipartFile("file", "bad.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getInsertedRows()).isEqualTo(0);
        assertThat(result.getErrors()).hasSize(2);
        verify(txRepo, never()).save(any(TransactionEntity.class));
    }
}
