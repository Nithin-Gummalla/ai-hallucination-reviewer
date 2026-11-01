package com.reviewer.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanResponse {
    private Long scanId;
    private String repoUrl;
    private String owner;
    private String repo;
    private String defaultBranch;
    private int totalFiles;
    private long totalBytes;
    private List<FetchedFileDto> filesFetched = new ArrayList<>();
    private List<FindingDto> findings = new ArrayList<>();
    private String status;
    private String message;
    private LocalDateTime scannedAt;

    public ScanResponse() {
        this.scannedAt = LocalDateTime.now();
    }

    public Long getScanId() {
        return scanId;
    }

    public void setScanId(Long scanId) {
        this.scanId = scanId;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public List<FetchedFileDto> getFilesFetched() {
        return filesFetched;
    }

    public void setFilesFetched(List<FetchedFileDto> filesFetched) {
        this.filesFetched = filesFetched;
    }

    public List<FindingDto> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingDto> findings) {
        this.findings = findings;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }
}
