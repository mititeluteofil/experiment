package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
    @NotBlank String message,
    String sessionId
) {}
