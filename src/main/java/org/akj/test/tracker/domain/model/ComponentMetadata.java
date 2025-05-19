package org.akj.test.tracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentMetadata {
    private String name;

    private String eimId;

    private String sourceCodeUrl;
}
