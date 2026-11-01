package com.reviewer.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scan_records")
public class ScanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String repoUrl;

    @Column(nullable = false, length = 150)
    private String owner;

    @Column(nullable = false, length = 150)
    private String repoName;

    private String defaultBranch;

    @Column(nullable = false)
    private LocalDateTime scannedAt;

    private String status;

    private int totalFiles;

    private long totalBytes;

    @OneToMany(mappedBy = "scanRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FindingRecord> findings = new ArrayList<>();

    public ScanRecord() {
        this.scannedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public LocalDateTime getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(LocalDateTime scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<FindingRecord> getFindings() {
        return findings;
    }

    public void setFindings(List<FindingRecord> findings) {
        this.findings = findings;
    }

    public void addFinding(FindingRecord finding) {
        findings.add(finding);
        finding.setScanRecord(this);
    }
}
