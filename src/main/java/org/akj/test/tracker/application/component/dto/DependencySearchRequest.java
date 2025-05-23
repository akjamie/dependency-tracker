package org.akj.test.tracker.application.component.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencySearchRequest {
    private String q;
    private String componentId;
    private String name;
    private String sourceCodeUrl;
    private String branch;
    private String runtimeVersion;
    private String compiler;
    private String language;
    private String buildManager;
    
    @Builder.Default
    private int page = 1;
    
    @Builder.Default
    private int size = 20;
    
    private String sort;
    private Sort.Direction order;
}