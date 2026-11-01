package com.reviewer.dto;

import com.reviewer.model.FindingLabel;

public class FindingDto {
    private Long id;
    private String file;
    private Integer startLine;
    private Integer endLine;
    private String quotedSnippet;
    private String category;
    private String severity;
    private String explanation;
    private FindingLabel label;
    private String evidence;

    public FindingDto() {
    }

    public FindingDto(Long id, String file, Integer startLine, Integer endLine, String quotedSnippet,
                      String category, String severity, String explanation, FindingLabel label, String evidence) {
        this.id = id;
        this.file = file;
        this.startLine = startLine;
        this.endLine = endLine;
        this.quotedSnippet = quotedSnippet;
        this.category = category;
        this.severity = severity;
        this.explanation = explanation;
        this.label = label;
        this.evidence = evidence;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getQuotedSnippet() {
        return quotedSnippet;
    }

    public void setQuotedSnippet(String quotedSnippet) {
        this.quotedSnippet = quotedSnippet;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public FindingLabel getLabel() {
        return label;
    }

    public void setLabel(FindingLabel label) {
        this.label = label;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
