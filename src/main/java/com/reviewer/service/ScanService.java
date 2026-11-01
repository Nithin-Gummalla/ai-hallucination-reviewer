package com.reviewer.service;

import com.reviewer.client.FetchedRepoData;
import com.reviewer.client.GithubClient;
import com.reviewer.dto.FetchedFile;
import com.reviewer.dto.FetchedFileDto;
import com.reviewer.dto.ScanRequest;
import com.reviewer.dto.ScanResponse;
import com.reviewer.model.ScanRecord;
import com.reviewer.repository.ScanRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final GithubClient githubClient;
    private final ScanRecordRepository scanRecordRepository;

    public ScanService(GithubClient githubClient, ScanRecordRepository scanRecordRepository) {
        this.githubClient = githubClient;
        this.scanRecordRepository = scanRecordRepository;
    }

    @Transactional
    public ScanResponse scanRepository(ScanRequest request) {
        if (request == null || request.getRepoUrl() == null) {
            throw new IllegalArgumentException("Scan request and repoUrl cannot be null.");
        }

        log.info("Starting scan for repository: {}", request.getRepoUrl());

        // 1. Fetch files safely via GitHub REST API
        FetchedRepoData repoData = githubClient.fetchRepositoryFiles(request.getRepoUrl());

        // 2. Persist scan record in H2
        ScanRecord record = new ScanRecord();
        record.setRepoUrl(request.getRepoUrl());
        record.setOwner(repoData.owner());
        record.setRepoName(repoData.repo());
        record.setDefaultBranch(repoData.defaultBranch());
        record.setStatus("FETCH_COMPLETE");
        record.setTotalFiles(repoData.files().size());
        record.setTotalBytes(repoData.totalBytes());
        record.setScannedAt(LocalDateTime.now());
        record = scanRecordRepository.save(record);

        // 3. Assemble response
        ScanResponse response = new ScanResponse();
        response.setScanId(record.getId());
        response.setRepoUrl(request.getRepoUrl());
        response.setOwner(repoData.owner());
        response.setRepo(repoData.repo());
        response.setDefaultBranch(repoData.defaultBranch());
        response.setTotalFiles(repoData.files().size());
        response.setTotalBytes(repoData.totalBytes());
        response.setStatus("FETCH_COMPLETE");
        response.setMessage(String.format("Successfully fetched %d file(s) (%d bytes) from %s/%s.",
                repoData.files().size(), repoData.totalBytes(), repoData.owner(), repoData.repo()));
        response.setScannedAt(record.getScannedAt());

        List<FetchedFileDto> fileSummaries = new ArrayList<>();
        for (FetchedFile file : repoData.files()) {
            fileSummaries.add(new FetchedFileDto(file.getPath(), file.getSize()));
        }
        response.setFilesFetched(fileSummaries);

        // Findings will be populated in Milestones 2-4
        response.setFindings(new ArrayList<>());

        log.info("Completed file fetch for {}/{} ({} files, {} bytes)",
                repoData.owner(), repoData.repo(), repoData.files().size(), repoData.totalBytes());

        return response;
    }
}
