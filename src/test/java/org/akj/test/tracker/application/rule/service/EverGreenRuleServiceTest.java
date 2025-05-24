package org.akj.test.tracker.application.rule.service;

import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.domain.common.model.LanguageType;
import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.akj.test.tracker.infrastructure.storage.rule.EverGreenRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EverGreenRuleServiceTest {

    @Mock
    private EverGreenRuleRepository everGreenRuleRepository;

    @Mock
    private EverGreenRuleMapper everGreenRuleMapper;

    @InjectMocks
    private EverGreenRuleService everGreenRuleService;

    private EverGreenRuleDto sampleRuleDto;
    private EverGreenRule sampleRule;

    @BeforeEach
    void setUp() {
        sampleRuleDto = EverGreenRuleDto.builder()
                .id("test-id")
                .name("Test Rule")
                .description("Test Description")
                .language(LanguageType.JAVA)
                .status("ACTIVE")
                .build();

        sampleRule = new EverGreenRule();
        sampleRule.setId("test-id");
        sampleRule.setName("Test Rule");
        sampleRule.setDescription("Test Description");
        sampleRule.setLanguage(LanguageType.JAVA);
        sampleRule.setStatus("ACTIVE");
        sampleRule.setCreatedAt(LocalDateTime.now());
        sampleRule.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void addRule_Success() {
        when(everGreenRuleMapper.toDomain(any(EverGreenRuleDto.class))).thenReturn(sampleRule);
        when(everGreenRuleRepository.save(any(EverGreenRule.class))).thenReturn(sampleRule);
        when(everGreenRuleMapper.toDto(any(EverGreenRule.class))).thenReturn(sampleRuleDto);
        when(everGreenRuleRepository.existsByName(anyString())).thenReturn(false);

        EverGreenRuleDto result = everGreenRuleService.addRule(sampleRuleDto);

        assertNotNull(result);
        assertEquals(sampleRuleDto.getId(), result.getId());
        verify(everGreenRuleRepository, times(1)).save(any(EverGreenRule.class));
    }

    @Test
    void addRule_DuplicateName() {
        when(everGreenRuleRepository.existsByName(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> everGreenRuleService.addRule(sampleRuleDto));
        verify(everGreenRuleRepository, never()).save(any(EverGreenRule.class));
    }

    @Test
    void updateRule_Success() {
        when(everGreenRuleRepository.findById(anyString())).thenReturn(Optional.of(sampleRule));
        when(everGreenRuleRepository.save(any(EverGreenRule.class))).thenReturn(sampleRule);
        when(everGreenRuleMapper.toDto(any(EverGreenRule.class))).thenReturn(sampleRuleDto);
        when(everGreenRuleRepository.existsByName(anyString())).thenReturn(false);

        EverGreenRuleDto result = everGreenRuleService.updateRule(sampleRuleDto);

        assertNotNull(result);
        assertEquals(sampleRuleDto.getId(), result.getId());
        verify(everGreenRuleMapper, times(1)).updateDomainFromDto(any(), any());
        verify(everGreenRuleRepository, times(1)).save(any(EverGreenRule.class));
    }

    @Test
    void updateRule_NotFound() {
        when(everGreenRuleRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> everGreenRuleService.updateRule(sampleRuleDto));
        verify(everGreenRuleRepository, never()).save(any(EverGreenRule.class));
    }

    @Test
    void getRuleById_Success() {
        when(everGreenRuleRepository.findById(anyString())).thenReturn(Optional.of(sampleRule));
        when(everGreenRuleMapper.toDto(any(EverGreenRule.class))).thenReturn(sampleRuleDto);

        EverGreenRuleDto result = everGreenRuleService.getRuleById("test-id");

        assertNotNull(result);
        assertEquals(sampleRuleDto.getId(), result.getId());
    }

    @Test
    void getRuleById_NotFound() {
        when(everGreenRuleRepository.findById(anyString())).thenReturn(Optional.empty());

        EverGreenRuleDto result = everGreenRuleService.getRuleById("non-existent");

        assertNull(result);
    }

    @Test
    void searchRules_Success() {
        List<EverGreenRule> rules = Arrays.asList(sampleRule);
        when(everGreenRuleRepository.searchRules(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(rules);
        when(everGreenRuleMapper.toDto(any(EverGreenRule.class))).thenReturn(sampleRuleDto);

        EverGreenRuleSearchResponse response = everGreenRuleService.searchRules(
                "test", ProgramLanguage.JAVA, RuleStatus.ACTIVE, LocalDate.now(), LocalDate.now());

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertFalse(response.getRules().isEmpty());
        assertEquals(1, response.getRules().size());
        assertEquals(sampleRuleDto.getId(), response.getRules().get(0).getId());
        assertEquals(1, response.getTotal());
    }

    @Test
    void searchRules_EmptyResult() {
        when(everGreenRuleRepository.searchRules(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Arrays.asList());

        EverGreenRuleSearchResponse response = everGreenRuleService.searchRules(
                "test", ProgramLanguage.JAVA, RuleStatus.ACTIVE, LocalDate.now(), LocalDate.now());

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertTrue(response.getRules().isEmpty());
        assertEquals(0, response.getTotal());
    }

    @Test
    void searchRules_NullDates() {
        List<EverGreenRule> rules = Arrays.asList(sampleRule);
        when(everGreenRuleRepository.searchRules(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(rules);
        when(everGreenRuleMapper.toDto(any(EverGreenRule.class))).thenReturn(sampleRuleDto);

        EverGreenRuleSearchResponse response = everGreenRuleService.searchRules(
                "test", ProgramLanguage.JAVA, RuleStatus.ACTIVE, null, null);

        assertNotNull(response);
        assertNotNull(response.getRules());
        assertFalse(response.getRules().isEmpty());
        assertEquals(1, response.getRules().size());
        assertEquals(sampleRuleDto.getId(), response.getRules().get(0).getId());
        assertEquals(1, response.getTotal());
    }
} 