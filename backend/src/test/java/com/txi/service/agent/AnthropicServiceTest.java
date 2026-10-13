package com.txi.service.agent;

import com.txi.service.analytics.CategorySpendDto;
import com.txi.service.tools.DateRangeRequest;
import com.txi.service.tools.TransactionInsightsToolService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AnthropicServiceTest {

    @Test
    public void handleQuery_withoutApiKey_returnsConfigurationMessage() {
        TransactionInsightsToolService toolService = mock(TransactionInsightsToolService.class);
        AnthropicService service = new AnthropicService(toolService, "");

        assertThat(service.handleQuery("What was my spend by category?")).contains("Anthropic API key");
    }

    @Test
    public void buildDataContextSummary_includesLoadedCategoryData() {
        TransactionInsightsToolService toolService = mock(TransactionInsightsToolService.class);
        AnthropicService service = new AnthropicService(toolService, "key", "workspace", "", "https://api.anthropic.com", "claude-3-5-sonnet-latest");

        String summary = service.buildDataContextSummary(List.of(
                new CategorySpendDto("Groceries", new BigDecimal("125.50")),
                new CategorySpendDto("Dining", new BigDecimal("80.25"))
        ));

        assertThat(summary).contains("Loaded transaction data summary").contains("Groceries").contains("$125.50").contains("Dining");
    }

    @Test
    public void buildDataContextSummary_includesRequestedPeriodLabel() {
        TransactionInsightsToolService toolService = mock(TransactionInsightsToolService.class);
        AnthropicService service = new AnthropicService(toolService, "key", "workspace", "", "https://api.anthropic.com", "claude-3-5-sonnet-latest");

        String summary = service.buildDataContextSummary(List.of(
                new CategorySpendDto("Groceries", new BigDecimal("125.50"))
        ), new DateRangeRequest(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)));

        assertThat(summary).contains("April 2026");
    }
}
