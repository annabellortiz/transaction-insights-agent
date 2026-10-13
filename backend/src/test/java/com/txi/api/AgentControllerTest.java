package com.txi.api;

import com.txi.service.agent.AnthropicService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentControllerTest {

    @Test
    public void query_returnsAgentResponse() {
        AnthropicService service = mock(AnthropicService.class);
        when(service.handleQuery("Total spend by category")).thenReturn("Here is the spend summary.");

        AgentController controller = new AgentController(service);
        ResponseEntity<AgentQueryResponse> response = controller.query(new AgentQueryRequest("Total spend by category"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().response()).isEqualTo("Here is the spend summary.");
    }
}
