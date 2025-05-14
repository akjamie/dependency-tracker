package org.akj.test.tracker.domain;

public class Source {
    private Integer id;
    private String teamName;
    private String eimId;
    private SourceType sourceType;
    private String gitUrl;
    private String gitBranch; // if not specified, take the main or master branch

}
