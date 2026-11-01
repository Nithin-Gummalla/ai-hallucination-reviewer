package com.reviewer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.dto.FetchedFileDto;
import com.reviewer.dto.ScanRequest;
import com.reviewer.dto.ScanResponse;
import com.reviewer.exception.GlobalExceptionHandler;
import com.reviewer.exception.InvalidRepoUrlException;
import com.reviewer.exception.RateLimitExceededException;
import com.reviewer.exception.RepoNotFoundException;
import com.reviewer.service.ScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScanController.class)
@Import(GlobalExceptionHandler.class)
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScanService scanService;

    @Test
    @DisplayName("POST /api/scan with valid URL returns 200 and scan details")
    void testScanSuccess() throws Exception {
        ScanResponse response = new ScanResponse();
        response.setScanId(1L);
        response.setRepoUrl("https://github.com/spring-projects/spring-petclinic");
        response.setOwner("spring-projects");
        response.setRepo("spring-petclinic");
        response.setDefaultBranch("main");
        response.setTotalFiles(2);
        response.setTotalBytes(15000);
        response.setStatus("FETCH_COMPLETE");
        response.setFilesFetched(List.of(
                new FetchedFileDto("pom.xml", 5000),
                new FetchedFileDto("src/main/java/org/springframework/samples/petclinic/PetClinicApplication.java", 10000)
        ));

        when(scanService.scanRepository(any(ScanRequest.class))).thenReturn(response);

        ScanRequest request = new ScanRequest("https://github.com/spring-projects/spring-petclinic");

        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").value(1))
                .andExpect(jsonPath("$.owner").value("spring-projects"))
                .andExpect(jsonPath("$.repo").value("spring-petclinic"))
                .andExpect(jsonPath("$.totalFiles").value(2))
                .andExpect(jsonPath("$.filesFetched[0].path").value("pom.xml"))
                .andExpect(jsonPath("$.status").value("FETCH_COMPLETE"));
    }

    @Test
    @DisplayName("POST /api/scan with SSRF/invalid URL returns 400 Bad Request")
    void testScanInvalidUrl() throws Exception {
        when(scanService.scanRepository(any(ScanRequest.class)))
                .thenThrow(new InvalidRepoUrlException("Invalid GitHub repository URL"));

        ScanRequest request = new ScanRequest("http://169.254.169.254/latest/meta-data");

        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid GitHub repository URL"));
    }

    @Test
    @DisplayName("POST /api/scan for missing repo returns 404 Not Found")
    void testScanRepoNotFound() throws Exception {
        when(scanService.scanRepository(any(ScanRequest.class)))
                .thenThrow(new RepoNotFoundException("Repository not found or is private: fake/repo"));

        ScanRequest request = new ScanRequest("https://github.com/fake/repo");

        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Repository not found or is private: fake/repo"));
    }

    @Test
    @DisplayName("POST /api/scan when rate limited returns 429 Too Many Requests")
    void testScanRateLimit() throws Exception {
        when(scanService.scanRepository(any(ScanRequest.class)))
                .thenThrow(new RateLimitExceededException("GitHub API rate limit exceeded"));

        ScanRequest request = new ScanRequest("https://github.com/spring-projects/spring-petclinic");

        mockMvc.perform(post("/api/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("GitHub API rate limit exceeded"));
    }
}
