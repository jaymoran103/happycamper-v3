package com.echo.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the end-to-end behavior of {@code POST /process}: two CSV uploads in,
 * JSON response out with both the enriched CSV body and an assertion report.
 *
 * The fixtures in {@code src/test/resources} are sourced from the core test rosters so
 * the assertion and feature logic exercised here mirrors the desktop happy path.
 */
@SpringBootTest
@TestPropertySource(properties = {"spring.main.banner-mode=off"})
class ProcessControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("POST /process returns assertion report + enriched CSV for valid uploads")
    void processReturnsAssertionReportAndEnrichedCsv() throws Exception {
        MockMultipartFile camperFile = loadFixture("camperFile", "campers.csv", "testCamperRoster.csv");
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        mockMvc.perform(multipart("/process")
                        .file(camperFile)
                        .file(activityFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assertions.summary.total").value(5))
                .andExpect(jsonPath("$.assertions.results", hasSize(5)))
                // Activity-based assertions should evaluate (PASS or FAIL) when ActivityFeature ran.
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'all_campers_have_max_rounds')].status")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo("SKIPPED")))))
                // The enriched CSV string carries the canonical header row and at least one camper.
                .andExpect(jsonPath("$.enrichedCsv", containsString("First Name")))
                .andExpect(jsonPath("$.enrichedCsv", containsString("Round 1")));
    }

    @Test
    @DisplayName("POST /process rejects missing camperFile with 400")
    void processRejectsMissingCamperFile() throws Exception {
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        mockMvc.perform(multipart("/process").file(activityFile))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /process honors explicit feature toggles")
    void processHonorsFeatureToggles() throws Exception {
        MockMultipartFile camperFile = loadFixture("camperFile", "campers.csv", "testCamperRoster.csv");
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        // Only request the activity feature — preference/swim/medical should not run, so
        // their assertions should report SKIPPED.
        mockMvc.perform(multipart("/process")
                        .file(camperFile)
                        .file(activityFile)
                        .param("features", "activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'no_unrequested_activities')].status")
                        .value(org.hamcrest.Matchers.hasItem("SKIPPED")))
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'no_swim_conflicts')].status")
                        .value(org.hamcrest.Matchers.hasItem("SKIPPED")));
    }

    private static MockMultipartFile loadFixture(String formField, String filename, String classpathResource) throws Exception {
        try (var in = new ClassPathResource(classpathResource).getInputStream()) {
            return new MockMultipartFile(formField, filename, "text/csv", in.readAllBytes());
        }
    }
}
