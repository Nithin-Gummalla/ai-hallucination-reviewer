package com.reviewer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.dto.FetchedFile;
import com.reviewer.exception.InvalidRepoUrlException;
import com.reviewer.exception.OversizedRepoException;
import com.reviewer.exception.RateLimitExceededException;
import com.reviewer.exception.RepoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https://github\\.com/([a-zA-Z0-9][-a-zA-Z0-9_.]*)/([a-zA-Z0-9_.-]+?)(?:\\.git)?/?$"
    );

    public static final long MAX_FILE_SIZE_BYTES = 200 * 1024; // 200 KB
    public static final long MAX_TOTAL_SIZE_BYTES = 1024 * 1024; // 1 MB
    public static final int MAX_JAVA_FILES = 25;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    public GithubClient(@Value("${github.api.token:}") String githubToken,
                        @Value("${github.api.base-url:https://api.github.com}") String baseUrl,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "AI-Hallucination-Reviewer")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json");

        if (githubToken != null && !githubToken.trim().isEmpty()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken.trim());
        }

        this.restClient = builder.build();
    }

    // Constructor for testing with custom RestClient
    public GithubClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Validates and extracts owner and repo name from the repo URL.
     * Prevents SSRF by strictly accepting only https://github.com/{owner}/{repo}.
     */
    public RepoCoordinates validateAndParseUrl(String repoUrl) {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            throw new InvalidRepoUrlException("Repository URL cannot be empty. Expected format: https://github.com/{owner}/{repo}");
        }

        String trimmed = repoUrl.trim();
        Matcher matcher = GITHUB_URL_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            throw new InvalidRepoUrlException("Invalid GitHub repository URL: '" + trimmed +
                    "'. Only URLs of the form https://github.com/{owner}/{repo} are accepted.");
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }

        if (owner.equals(".") || owner.equals("..") || repo.equals(".") || repo.equals("..") || repo.isEmpty()) {
            throw new InvalidRepoUrlException("Invalid GitHub repository URL coordinates.");
        }

        return new RepoCoordinates(owner, repo);
    }

    /**
     * Fetches repository files adhering to all constraints:
     * - Only pom.xml, up to 25 .java files, and application.properties/yml
     * - Max 200 KB per file
     * - Max 1 MB total
     * - Handles missing repo, rate limit, and oversized repos gracefully
     */
    public FetchedRepoData fetchRepositoryFiles(String repoUrl) {
        RepoCoordinates coords = validateAndParseUrl(repoUrl);
        String owner = coords.owner();
        String repo = coords.repo();

        // 1. Fetch repo metadata to determine default branch
        String defaultBranch = fetchDefaultBranch(owner, repo);

        // 2. Fetch git tree recursively
        JsonNode treeNode = fetchGitTree(owner, repo, defaultBranch);

        // 3. Filter files
        List<TreeFileEntry> candidateFiles = selectFilesToFetch(treeNode);

        // 4. Fetch contents
        List<FetchedFile> fetchedFiles = new ArrayList<>();
        long totalBytes = 0;

        for (TreeFileEntry fileEntry : candidateFiles) {
            FetchedFile file = fetchFileContent(owner, repo, fileEntry.path(), defaultBranch);
            if (file == null) {
                continue;
            }

            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                if (file.getPath().endsWith("pom.xml")) {
                    throw new OversizedRepoException("pom.xml exceeds the 200 KB limit (" + file.getSize() + " bytes).");
                }
                log.warn("Skipping file {} because size {} exceeds 200 KB limit", file.getPath(), file.getSize());
                continue;
            }

            if (totalBytes + file.getSize() > MAX_TOTAL_SIZE_BYTES) {
                log.info("Reached 1 MB total limit while fetching files. Stopping at {} files.", fetchedFiles.size());
                break;
            }

            fetchedFiles.add(file);
            totalBytes += file.getSize();
        }

        // Verify that pom.xml was fetched
        boolean hasPom = fetchedFiles.stream().anyMatch(f -> f.getPath().endsWith("pom.xml"));
        if (!hasPom) {
            throw new InvalidRepoUrlException("Repository does not contain a valid pom.xml. Only Java/Maven projects are supported.");
        }

        return new FetchedRepoData(owner, repo, defaultBranch, fetchedFiles, totalBytes);
    }

    private String fetchDefaultBranch(String owner, String repo) {
        try {
            String response = restClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        int code = res.getStatusCode().value();
                        if (code == 404) {
                            throw new RepoNotFoundException("Repository not found or is private: " + owner + "/" + repo);
                        } else if (code == 403 || code == 429) {
                            throw new RateLimitExceededException("GitHub API rate limit exceeded. Please try again later or configure GITHUB_TOKEN.");
                        } else {
                            throw new RuntimeException("GitHub API returned HTTP " + code);
                        }
                    })
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            if (root.has("default_branch")) {
                return root.get("default_branch").asText();
            }
            return "main";
        } catch (HttpClientErrorException.NotFound e) {
            throw new RepoNotFoundException("Repository not found or is private: " + owner + "/" + repo);
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.TooManyRequests e) {
            throw new RateLimitExceededException("GitHub API rate limit exceeded. Please try again later or configure GITHUB_TOKEN.");
        } catch (RepoNotFoundException | RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch repository metadata for {}/{}", owner, repo, e);
            throw new RuntimeException("Failed to fetch repository details: " + e.getMessage(), e);
        }
    }

    private JsonNode fetchGitTree(String owner, String repo, String defaultBranch) {
        try {
            String response = restClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, defaultBranch)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        int code = res.getStatusCode().value();
                        if (code == 404) {
                            throw new RepoNotFoundException("Repository git tree not found: " + owner + "/" + repo);
                        } else if (code == 403 || code == 429) {
                            throw new RateLimitExceededException("GitHub API rate limit exceeded while fetching repository tree.");
                        } else {
                            throw new RuntimeException("GitHub API returned HTTP " + code + " for tree");
                        }
                    })
                    .body(String.class);

            return objectMapper.readTree(response);
        } catch (RepoNotFoundException | RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch git tree for {}/{}", owner, repo, e);
            throw new RuntimeException("Failed to fetch git tree: " + e.getMessage(), e);
        }
    }

    private List<TreeFileEntry> selectFilesToFetch(JsonNode treeNode) {
        if (treeNode == null || !treeNode.has("tree") || !treeNode.get("tree").isArray()) {
            throw new InvalidRepoUrlException("Repository tree is empty or invalid.");
        }

        JsonNode treeArray = treeNode.get("tree");
        TreeFileEntry pomFile = null;
        List<TreeFileEntry> configFiles = new ArrayList<>();
        List<TreeFileEntry> javaFiles = new ArrayList<>();

        for (JsonNode item : treeArray) {
            String type = item.has("type") ? item.get("type").asText() : "";
            if (!"blob".equals(type)) {
                continue;
            }

            String path = item.has("path") ? item.get("path").asText() : "";
            long size = item.has("size") ? item.get("size").asLong(0) : 0;

            // Check if oversized before even queuing
            if (size > MAX_FILE_SIZE_BYTES) {
                if ("pom.xml".equals(path) || path.endsWith("/pom.xml")) {
                    throw new OversizedRepoException("pom.xml exceeds the 200 KB limit (" + size + " bytes).");
                }
                continue;
            }

            String lowerPath = path.toLowerCase(Locale.ROOT);
            if (path.equals("pom.xml")) {
                pomFile = new TreeFileEntry(path, size);
            } else if (pomFile == null && path.endsWith("/pom.xml")) {
                pomFile = new TreeFileEntry(path, size);
            } else if (lowerPath.endsWith("application.properties") ||
                       lowerPath.endsWith("application.yml") ||
                       lowerPath.endsWith("application.yaml")) {
                configFiles.add(new TreeFileEntry(path, size));
            } else if (lowerPath.endsWith(".java")) {
                javaFiles.add(new TreeFileEntry(path, size));
            }
        }

        if (pomFile == null) {
            throw new InvalidRepoUrlException("Repository does not contain a pom.xml file. Only Java/Maven projects are supported.");
        }

        // Sort Java files deterministically: prefer src/main/java first, then alphabetized
        javaFiles.sort((a, b) -> {
            boolean aMain = a.path().contains("src/main/java");
            boolean bMain = b.path().contains("src/main/java");
            if (aMain && !bMain) return -1;
            if (!aMain && bMain) return 1;
            return a.path().compareTo(b.path());
        });

        // Limit to 25 Java files
        List<TreeFileEntry> selectedJavaFiles = javaFiles.stream()
                .limit(MAX_JAVA_FILES)
                .toList();

        List<TreeFileEntry> result = new ArrayList<>();
        result.add(pomFile);
        result.addAll(configFiles);
        result.addAll(selectedJavaFiles);

        return result;
    }

    private FetchedFile fetchFileContent(String owner, String repo, String path, String ref) {
        try {
            String response = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={ref}", owner, repo, path, ref)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, res) -> {
                        int code = res.getStatusCode().value();
                        if (code == 403 || code == 429) {
                            throw new RateLimitExceededException("GitHub API rate limit exceeded while fetching file content.");
                        }
                        log.warn("Failed to fetch file content for {}: HTTP {}", path, code);
                    })
                    .body(String.class);

            if (response == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!root.has("content")) {
                return null;
            }

            String base64Content = root.get("content").asText().replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(base64Content);
            String content = new String(decoded, StandardCharsets.UTF_8);

            return new FetchedFile(path, content, decoded.length);
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error fetching content for file {}: {}", path, e.getMessage());
            return null;
        }
    }

    private record TreeFileEntry(String path, long size) {}
}
