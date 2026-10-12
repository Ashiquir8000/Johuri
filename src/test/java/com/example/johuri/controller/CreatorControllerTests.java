package com.example.johuri.controller;

import com.example.johuri.DTO.MediaKitPublicResponse;
import com.example.johuri.service.CreatorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class CreatorControllerTests {

    @Test
    void getCreatorsReturnsApiResponseEnvelope() throws Exception {
        CreatorService creatorService = mock(CreatorService.class);
        when(creatorService.getCreators()).thenReturn(List.of(
                MediaKitPublicResponse.builder()
                        .name("Demo Creator")
                        .youtubeHandle("@demo")
                        .verificationStatus("UNCLAIMED")
                        .servicePackages(List.of())
                        .build()
        ));
        MockMvc mockMvc = standaloneSetup(new CreatorController(creatorService)).build();

        mockMvc.perform(get("/api/v1/creators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Demo Creator"))
                .andExpect(jsonPath("$.data[0].youtubeHandle").value("@demo"));
    }
}
