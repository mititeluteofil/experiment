package com.example.demo.controller;

import com.example.demo.agent.AgentAssistant;
import com.example.demo.dto.AgentRequest;
import com.example.demo.dto.AgentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentAssistant agentAssistant;

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@Valid @RequestBody AgentRequest request) {
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : UUID.randomUUID().toString();
        String reply = agentAssistant.chat(sessionId, request.message());
        return ResponseEntity.ok(new AgentResponse(sessionId, reply));
    }
}
