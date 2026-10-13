package com.txi.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.txi.service.analytics.CategorySpendDto;
import com.txi.service.tools.DateRangeRequest;
import com.txi.service.tools.TransactionInsightsToolService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnthropicService {

    private final TransactionInsightsToolService toolService;
    private final String apiKey;
    private final String workspaceId;
    private final String organizationId;
    private final String apiUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AnthropicService(TransactionInsightsToolService toolService, String apiKey) {
        this(toolService, apiKey, "", "", "https://api.anthropic.com", "claude-3-5-sonnet-latest");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AnthropicService(
            TransactionInsightsToolService toolService,
            @Value("${anthropic.apiKey:}") String apiKey,
            @Value("${anthropic.workspaceId:}") String workspaceId,
            @Value("${anthropic.organizationId:}") String organizationId,
            @Value("${anthropic.apiUrl:https://api.anthropic.com}") String apiUrl,
            @Value("${anthropic.model:claude-3-5-sonnet-latest}") String model) {
        this.toolService = toolService;
        this.apiKey = apiKey;
        this.workspaceId = workspaceId;
        this.organizationId = organizationId;
        this.apiUrl = apiUrl;
        this.model = model;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String handleQuery(String userQuery) {
        if (apiKey == null || apiKey.isBlank()) {
            return "Anthropic API key is not configured. Add ANTHROPIC_API_KEY to enable agentic analysis.";
        }

        String scopeHeaderName = null;
        String scopeHeaderValue = null;
        if (workspaceId != null && !workspaceId.isBlank()) {
            scopeHeaderName = "anthropic-workspace-id";
            scopeHeaderValue = workspaceId;
        } else if (organizationId != null && !organizationId.isBlank()) {
            scopeHeaderName = "anthropic-organization-id";
            scopeHeaderValue = organizationId;
        }

        if (scopeHeaderName == null || scopeHeaderValue == null || scopeHeaderValue.isBlank()) {
            return "Anthropic scope ID is not configured. Add ANTHROPIC_WORKSPACE_ID or ANTHROPIC_ORGANIZATION_ID for a scoped key.";
        }

        try {
            DateRangeRequest requestRange = resolveDateRange(userQuery);
            List<CategorySpendDto> loadedSpendByCategory = toolService.sumByCategory(requestRange);
            String dataContextSummary = buildDataContextSummary(loadedSpendByCategory, requestRange);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "max_tokens", 512,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", buildPrompt(userQuery, dataContextSummary)
                            ))
                    ))
            ));

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header(scopeHeaderName, scopeHeaderValue);

            HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "Anthropic request failed with status " + response.statusCode() + ": " + response.body();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (content.isArray()) {
                for (JsonNode item : content) {
                    if (item.has("type") && "text".equals(item.get("type").asText())) {
                        return item.path("text").asText();
                    }
                }
            }

            return root.path("error").path("message").asText("Anthropic response did not include text content.");
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Anthropic call failed: " + e.getMessage();
        }
    }

    String buildDataContextSummary(List<CategorySpendDto> categorySpends) {
        return buildDataContextSummary(categorySpends, null);
    }

    String buildDataContextSummary(List<CategorySpendDto> categorySpends, DateRangeRequest dateRange) {
        if (categorySpends == null || categorySpends.isEmpty()) {
            return "Loaded transaction data summary" + formatPeriodSuffix(dateRange) + ": no transaction rows are currently available in the app.";
        }

        String summary = categorySpends.stream()
                .map(item -> "- " + item.categoryName() + ": $" + item.totalSpend())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");

        return "Loaded transaction data summary" + formatPeriodSuffix(dateRange) + " from the app database:\n" + summary;
    }

    private String formatPeriodSuffix(DateRangeRequest dateRange) {
        if (dateRange == null || dateRange.start() == null || dateRange.end() == null) {
            return "";
        }

        String startMonthName = dateRange.start().getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        String endMonthName = dateRange.end().getMonth().getDisplayName(TextStyle.FULL, Locale.US);
        int startYear = dateRange.start().getYear();
        int endYear = dateRange.end().getYear();

        if (dateRange.start().getYear() == dateRange.end().getYear()
                && dateRange.start().getMonthValue() == dateRange.end().getMonthValue()) {
            return " for " + startMonthName + " " + startYear;
        }

        String startLabel = dateRange.start().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US));
        String endLabel = dateRange.end().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US));
        return " for " + startMonthName + " " + startYear + " to " + endMonthName + " " + endYear + " (" + startLabel + " to " + endLabel + ")";
    }

    private DateRangeRequest resolveDateRange(String userQuery) {
        String query = userQuery == null ? "" : userQuery.toLowerCase(Locale.ROOT);

        Pattern monthYearPattern = Pattern.compile("(?:for|in)\\s+(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{4})");
        Matcher monthYearMatcher = monthYearPattern.matcher(query);
        if (monthYearMatcher.find()) {
            String monthName = monthYearMatcher.group(1);
            int year = Integer.parseInt(monthYearMatcher.group(2));
            int month = java.time.Month.valueOf(monthName.toUpperCase(Locale.ROOT)).getValue();
            YearMonth yearMonth = YearMonth.of(year, month);
            return new DateRangeRequest(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        }

        Pattern isoMonthPattern = Pattern.compile("(?:for|in)\\s+(\\d{4})-(\\d{2})");
        Matcher isoMonthMatcher = isoMonthPattern.matcher(query);
        if (isoMonthMatcher.find()) {
            int year = Integer.parseInt(isoMonthMatcher.group(1));
            int month = Integer.parseInt(isoMonthMatcher.group(2));
            YearMonth yearMonth = YearMonth.of(year, month);
            return new DateRangeRequest(yearMonth.atDay(1), yearMonth.atEndOfMonth());
        }

        return new DateRangeRequest(LocalDate.now().minusDays(365), LocalDate.now());
    }

    private String buildPrompt(String userQuery, String dataContextSummary) {
        return "You are a transaction insights agent for a personal finance dashboard in this application. " +
                "IMPORTANT: Use the loaded transaction data in this app. The app already has transaction records available and you must answer from that dataset. " +
                "Never say you do not have access to transaction data or ask the user to upload data when the app already contains transaction records. " +
                "Never respond with a generic refusal like 'I don't have access to any transaction data' or 'please share your transaction history.' " +
                "Always include the requested date range or month and year in the response, especially when the user asks for a specific month such as April 2026. " +
                "Use the following current data from the app database as your source of truth, and answer using it: " + dataContextSummary + " " +
                "If the user asks for totals, trends, largest or smallest transactions, or category breakdowns, compute from the loaded transaction dataset. " +
                "If the dataset has matching rows for the requested period, analyze them and provide a result. " +
                "If the dataset does not contain relevant rows, say exactly: 'No matching transactions found for the requested period.' " +
                "Answer in a polished, concise finance-report style with exactly these sections: " +
                "1) Summary: write one sentence that starts with 'For [Month Year], ...' and states the main total, clearly naming the period. " +
                "2) Breakdown: list the most relevant categories with dollar amounts, and include a final 'Total:' line that matches the summary total. " +
                "3) Insight: provide one short takeaway about the main driver of spending, such as the largest category or a standout trend. " +
                "Use bold labels like 'Summary', 'Breakdown', and 'Insight' and format numbers as dollars. Keep the tone professional and easy to read for a portfolio demo. " +
                "User request: " + userQuery;
    }
}
