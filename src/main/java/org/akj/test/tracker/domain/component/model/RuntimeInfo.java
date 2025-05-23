package org.akj.test.tracker.domain.component.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.RuntimeType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuntimeInfo {
    private RuntimeType type;

    private String version;
}
