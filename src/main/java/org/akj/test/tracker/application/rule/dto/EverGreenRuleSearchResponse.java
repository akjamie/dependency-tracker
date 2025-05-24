package org.akj.test.tracker.application.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EverGreenRuleSearchResponse {

    private EverGreenRuleSearchResponse.Metadata metadata;
    private List<EverGreenRuleDto> data;

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
