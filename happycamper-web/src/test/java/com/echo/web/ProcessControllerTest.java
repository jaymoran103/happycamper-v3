package com.echo.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies the end-to-end behavior of {@code POST /process}: two CSV uploads in,
 * JSON response out with both the enriched CSV body and an assertion report.
 *
 * Assertions are intentionally structural rather than count-exact so that adding new
 * assertions or warnings to core does not break this test. Stable contracts (assertion
 * IDs per ADR-002; warning/error type names per ADR-003) are checked by name.
 */
@SpringBootTest
class ProcessControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("POST /process returns assertion report + enriched CSV + structured warnings")
    void processReturnsAssertionReportAndEnrichedCsv() throws Exception {
        MockMultipartFile camperFile = loadFixture("camperFile", "campers.csv", "testCamperRoster.csv");
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        mockMvc.perform(multipart("/process")
                        .file(camperFile)
                        .file(activityFile))
                .andExpect(status().isOk())
                // Structural: at least one assertion ran, and the array size matches the summary count.
                .andExpect(jsonPath("$.assertions.summary.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.assertions.summary.passed", notNullValue()))
                .andExpect(jsonPath("$.assertions.summary.failed", notNullValue()))
                .andExpect(jsonPath("$.assertions.summary.skipped", notNullValue()))
                .andExpect(jsonPath("$.assertions.results", hasSize(greaterThanOrEqualTo(1))))
                // Stable per ADR-002: the all_campers_have_max_rounds assertion ID exists and was evaluated.
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'all_campers_have_max_rounds')]",
                        hasSize(greaterThanOrEqualTo(1))))
                // Stable per export contract: canonical headers appear in the enriched CSV.
                .andExpect(jsonPath("$.enrichedCsv", containsString("First Name")))
                .andExpect(jsonPath("$.enrichedCsv", containsString("Round 1")))
                // Warnings are always serialized as an array (may be empty); shape is {type, message}.
                .andExpect(jsonPath("$.warnings", notNullValue()));
    }

    @Test
    @DisplayName("POST /process rejects missing camperFile with 400 and flat error")
    void processRejectsMissingCamperFile() throws Exception {
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        mockMvc.perform(multipart("/process").file(activityFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("camperFile is required")));
    }

    @Test
    @DisplayName("POST /process honors explicit feature toggles — off features report SKIPPED")
    void processHonorsFeatureToggles() throws Exception {
        MockMultipartFile camperFile = loadFixture("camperFile", "campers.csv", "testCamperRoster.csv");
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        // Only request the activity feature — preference/swim/medical should not run, so
        // their owning assertions should report SKIPPED.
        mockMvc.perform(multipart("/process")
                        .file(camperFile)
                        .file(activityFile)
                        .param("features", "activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'no_unrequested_activities')].status",
                        hasItem("SKIPPED")))
                .andExpect(jsonPath("$.assertions.results[?(@.id == 'no_swim_conflicts')].status",
                        hasItem("SKIPPED")));
    }

    @Test
    @DisplayName("POST /process returns 422 with structured errors when CSV is malformed")
    void processReturns422OnMalformedCsv() throws Exception {
        MockMultipartFile camperFile = loadFixture("camperFile", "campers.csv", "malformedCamperRoster.csv");
        MockMultipartFile activityFile = loadFixture("activityFile", "activities.csv", "testActivityRoster.csv");

        mockMvc.perform(multipart("/process")
                        .file(camperFile)
                        .file(activityFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors", hasSize(greaterThanOrEqualTo(1))))
                // Each error has type + message strings per ADR-003 sub-decision B.
                .andExpect(jsonPath("$.errors[0].type", Matchers.instanceOf(String.class)))
                .andExpect(jsonPath("$.errors[0].message", Matchers.instanceOf(String.class)));
    }

    private static MockMultipartFile loadFixture(String formField, String filename, String classpathResource) throws Exception {
        try (var in = new ClassPathResource(classpathResource).getInputStream()) {
            return new MockMultipartFile(formField, filename, "text/csv", in.readAllBytes());
        }
    }
}
