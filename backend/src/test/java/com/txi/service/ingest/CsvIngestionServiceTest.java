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
    public void ingest_replacesExistingTransactionsBeforeInsertingNewFile() throws Exception {
        String csv = "Trans. Date,Post Date,Description,Amount,Category\n" +
                "02/01/2026,02/01/2026,INTERNET PAYMENT - THANK YOU,-482.39,Payments and Credits\n" +
                "02/21/2026,02/21/2026,DD *DOORDASH MCDONALDS 6506819470 CA,33.17,Restaurants\n";

        MockMultipartFile file = new MockMultipartFile("file", "discover.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(categoryRepo.findByName(anyString())).thenReturn(null);
        when(categoryRepo.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(System.nanoTime());
            return category;
        });
        when(txRepo.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getInsertedRows()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();
        verify(txRepo).deleteAll();
        verify(txRepo, times(2)).save(any(TransactionEntity.class));
    }

    @Test
    public void ingest_debitCreditCsv_supportsDebitAndCreditColumns() throws Exception {
        String csv = "Status,Date,Description,Debit,Credit\n" +
                "Cleared,09/07/2026,SCHNUCKS MID RIVERS ST. PETERS MO,4.19,\n" +
                "Cleared,08/30/2026,Zenni Optical, ,130.11\n";

        MockMultipartFile file = new MockMultipartFile("file", "bank.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(categoryRepo.findByName(anyString())).thenReturn(null);
        when(categoryRepo.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(System.nanoTime());
            return category;
        });
        when(txRepo.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getInsertedRows()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getErrors()).isEmpty();
        verify(txRepo, times(2)).save(any(TransactionEntity.class));
    }

    @Test
    public void ingest_csvWithoutCategory_assignsDefaultCategoryAndCountsSpend() throws Exception {
        String csv = "Date,Description,Amount\n" +
                "10/10/2026,LYFT,10.99\n" +
                "09/12/2026,AMAZON,27.22\n";

        MockMultipartFile file = new MockMultipartFile("file", "categoryless.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        when(categoryRepo.findByName(anyString())).thenReturn(null);
        when(categoryRepo.save(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(System.nanoTime());
            return category;
        });
        when(txRepo.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CsvIngestionResult result = ingestionService.ingest(file);

        assertThat(result.getInsertedRows()).isEqualTo(2);
        assertThat(result.getErrors()).isEmpty();
        verify(categoryRepo).findByName("Uncategorized");
        verify(txRepo, times(2)).save(any(TransactionEntity.class));
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
