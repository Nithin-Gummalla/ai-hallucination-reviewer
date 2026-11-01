package com.reviewer.client;

import com.reviewer.dto.FetchedFile;
import java.util.List;

public record FetchedRepoData(
        String owner,
        String repo,
        String defaultBranch,
        List<FetchedFile> files,
        long totalBytes
) {
}
