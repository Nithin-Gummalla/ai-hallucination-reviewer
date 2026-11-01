package com.reviewer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.exception.InvalidRepoUrlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class GithubClientTest {

    private GithubClient githubClient;

    @BeforeEach
    void setUp() {
        githubClient = new GithubClient("", "https://api.github.com", new ObjectMapper());
    }

    @Test
    @DisplayName("Valid GitHub URL without trailing slash or .git")
    void testValidUrlStandard() {
        RepoCoordinates coords = githubClient.validateAndParseUrl("https://github.com/spring-projects/spring-petclinic");
        assertEquals("spring-projects", coords.owner());
        assertEquals("spring-petclinic", coords.repo());
    }

    @Test
    @DisplayName("Valid GitHub URL with trailing slash")
    void testValidUrlTrailingSlash() {
        RepoCoordinates coords = githubClient.validateAndParseUrl("https://github.com/google/guava/");
        assertEquals("google", coords.owner());
        assertEquals("guava", coords.repo());
    }

    @Test
    @DisplayName("Valid GitHub URL with .git suffix")
    void testValidUrlDotGit() {
        RepoCoordinates coords = githubClient.validateAndParseUrl("https://github.com/apache/commons-lang.git");
        assertEquals("apache", coords.owner());
        assertEquals("commons-lang", coords.repo());
    }

    @Test
    @DisplayName("Valid GitHub URL with .git and trailing slash")
    void testValidUrlDotGitSlash() {
        RepoCoordinates coords = githubClient.validateAndParseUrl("https://github.com/square/okhttp.git/");
        assertEquals("square", coords.owner());
        assertEquals("okhttp", coords.repo());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "http://github.com/owner/repo",
            "https://evil.com/owner/repo",
            "https://github.com/owner",
            "https://github.com/owner/repo/subpath",
            "https://github.com/owner/repo/tree/main",
            "ftp://github.com/owner/repo",
            "file:///etc/passwd",
            "http://169.254.169.254/latest/meta-data/",
            "https://github.com/../evil/repo",
            "https://github.com/owner/../evil"
    })
    @DisplayName("Reject invalid URLs and SSRF attempts with InvalidRepoUrlException")
    void testInvalidUrlsRejected(String invalidUrl) {
        assertThrows(InvalidRepoUrlException.class, () -> githubClient.validateAndParseUrl(invalidUrl));
    }
}
