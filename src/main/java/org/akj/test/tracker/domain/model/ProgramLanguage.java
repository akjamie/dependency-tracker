package org.akj.test.tracker.domain.model;

public enum ProgramLanguage {
    JAVA("Java"),
    PYTHON("Python"),
    NODEJS("Node.js"),
    REACT("Reactjs"),
    VUE("Vuejs"),
    ANGULAR("Angular"),
    JAVASCRIPT("Javascript");

    private final String name;

    ProgramLanguage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
