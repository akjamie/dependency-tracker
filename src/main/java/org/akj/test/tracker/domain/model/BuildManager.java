package org.akj.test.tracker.domain.model;

public enum BuildManager {
    MAVEN("maven"),
    GRADLE("gradle"),
    NPM("npm"),
    YARN("yarn"),
    PIP("pip"),
    VITE("vite"),
    NEXT("next"),
    CRA("cra"),
    PNPM("pnpm");

    private final String name;

    BuildManager(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static BuildManager fromName(String name) {
        for (BuildManager buildManager : BuildManager.values()) {
            if (buildManager.getName().equalsIgnoreCase(name)) {
                return buildManager;
            }
        }
        throw new IllegalArgumentException("No BuildManager found with name: " + name);
    }
}
