package org.akj.test.tracker.domain.common.model;

public enum RuntimeType {
    // Java Runtimes
    JDK("jdk"),

    // Python Runtimes
    PYTHON("python"),

    // Node.js Runtimes
    NODE_JS("nodejs"),

    // Go Runtime
    GO("go"),

    // .NET Runtimes
    DOTNET("csharp"),

    // PHP Runtimes
    PHP("php"),

    // Kotlin Runtime
    KOTLIN("kotlin"),

    // Scala Runtime
    SCALA("scala"),

    // Groovy Runtime
    GROOVY("groovy"),

    UNKNOWN("unknown");


    private final String language;

    RuntimeType(String language) {
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public static RuntimeType fromLanguage(String language) {
        for (RuntimeType type : RuntimeType.values()) {
            if (type.language.equalsIgnoreCase(language)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown language: " + language);
    }
}