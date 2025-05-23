package org.akj.test.tracker.application.component.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DependencySearchResponse {
    private Metadata metadata;
    private List<ComponentAndDependencyDto> data;
    
    @Data
    @Builder
    public static class Metadata {
        // total count of dependencies
        private long total;
        // current page number
        private int page;
        // number of dependencies per page
        private int size;
        // total number of pages
        private int totalPages;
    }
}
