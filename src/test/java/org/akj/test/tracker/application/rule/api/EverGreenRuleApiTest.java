package org.akj.test.tracker.application.rule.api;

import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.application.rule.service.EverGreenRuleService;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EverGreenRuleApiTest {

    @Mock
    private EverGreenRuleService everGreenRuleService;

    @InjectMocks
    private EverGreenRuleApi everGreenRuleApi;

    private EverGreenRuleDto sampleRule;

    @BeforeEach
    void setUp() {
        sampleRule = EverGreenRuleDto.builder()
                .id("test-id")
                .name("Test Rule")
                .description("Test Description")
                .build();
    }

    @Test
    void addRule_Success() {
        when(everGreenRuleService.addRule(any(EverGreenRuleDto.class))).thenReturn(sampleRule);

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.addRule(sampleRule);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sampleRule, response.getBody().getData());
        verify(everGreenRuleService, times(1)).addRule(any(EverGreenRuleDto.class));
    }

    @Test
    void addRule_InvalidInput() {
        when(everGreenRuleService.addRule(any(EverGreenRuleDto.class)))
                .thenThrow(new IllegalArgumentException("Invalid rule data"));

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.addRule(sampleRule);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Invalid rule data"));
    }

    @Test
    void updateRule_Success() {
        when(everGreenRuleService.updateRule(any(EverGreenRuleDto.class))).thenReturn(sampleRule);

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.updateRule(sampleRule);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sampleRule, response.getBody().getData());
        verify(everGreenRuleService, times(1)).updateRule(any(EverGreenRuleDto.class));
    }

    @Test
    void updateRule_MissingId() {
        sampleRule.setId(null);

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.updateRule(sampleRule);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Rule ID is required"));
        verify(everGreenRuleService, never()).updateRule(any(EverGreenRuleDto.class));
    }

    @Test
    void getRuleById_Success() {
        when(everGreenRuleService.getRuleById("test-id")).thenReturn(sampleRule);

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.getRuleById("test-id");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(sampleRule, response.getBody().getData());
        verify(everGreenRuleService, times(1)).getRuleById("test-id");
    }

    @Test
    void getRuleById_NotFound() {
        when(everGreenRuleService.getRuleById("non-existent")).thenReturn(null);

        ResponseEntity<ApiResponse<EverGreenRuleDto>> response = everGreenRuleApi.getRuleById("non-existent");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Rule not found"));
    }

    @Test
    void searchRules_Success() {
        List<EverGreenRuleDto> rules = Arrays.asList(sampleRule);
        when(everGreenRuleService.searchRules(any(), any(), any(), any(), any())).thenReturn(rules);

        ResponseEntity<ApiResponse<List<EverGreenRuleDto>>> response = everGreenRuleApi.searchRules(
                "test", "ACTIVE", "JAVA", LocalDateTime.now(), LocalDateTime.now());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(rules, response.getBody().getData());
        verify(everGreenRuleService, times(1)).searchRules(any(), any(), any(), any(), any());
    }

    @Test
    void searchRules_InvalidDateRange() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(1);

        ResponseEntity<ApiResponse<List<EverGreenRuleDto>>> response = everGreenRuleApi.searchRules(
                "test", "ACTIVE", "JAVA", future, now);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("dateFrom must be before dateTo"));
        verify(everGreenRuleService, never()).searchRules(any(), any(), any(), any(), any());
    }

    @Test
    void searchRules_ServiceError() {
        when(everGreenRuleService.searchRules(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        ResponseEntity<ApiResponse<List<EverGreenRuleDto>>> response = everGreenRuleApi.searchRules(
                "test", "ACTIVE", "JAVA", LocalDateTime.now(), LocalDateTime.now());

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getMessage().contains("Failed to search rules"));
    }
} 