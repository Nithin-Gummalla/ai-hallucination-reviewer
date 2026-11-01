package com.reviewer.dto;

public class ScanRequest {
    private String repoUrl;

    public ScanRequest() {
    }

    public ScanRequest(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
