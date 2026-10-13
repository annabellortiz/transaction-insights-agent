package com.txi.api;

import com.txi.service.agent.AnthropicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentController {

    private final AnthropicService anthropicService;

    public AgentController(AnthropicService anthropicService) {
        this.anthropicService = anthropicService;
    }

    @PostMapping("/agent/query")
    public ResponseEntity<AgentQueryResponse> query(@RequestBody AgentQueryRequest request) {
        String response = anthropicService.handleQuery(request.query());
        return ResponseEntity.ok(new AgentQueryResponse(response));
    }
}
