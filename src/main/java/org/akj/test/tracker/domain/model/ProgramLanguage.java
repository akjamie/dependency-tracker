package org.akj.test.tracker.domain.model;

public enum ProgramLanguage {
    JAVA("Java"),
    PYTHON("Python"),
    NODEJS("Node.js");

    private final String name;

    ProgramLanguage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
