package org.akj.test.tracker.domain.common.model;

public enum ProgramLanguage {
    JAVA("Java"),
    PYTHON("Python"),
    NODEJS("Node.js"),
    REACT("Reactjs"),
    VUE("Vuejs"),
    ANGULAR("Angular"),
    JAVASCRIPT("Javascript"),
    UNKNOWN("Unknown");


    private final String name;

    ProgramLanguage(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ProgramLanguage fromName(String name) {
        for (ProgramLanguage programLanguage : ProgramLanguage.values()) {
            if (programLanguage.getName().equalsIgnoreCase(name)) {
                return programLanguage;
            }
        }
        throw new IllegalArgumentException("No ProgramLanguage found with name: " + name);
    }
}
