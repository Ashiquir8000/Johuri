package com.example.johuri.service;

import com.example.johuri.entity.CreatorProfile;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class YouTubeDataServiceImpl implements YouTubeDataService {

    private static final String CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";

    private final String apiKey;
    private final RestTemplate restTemplate;

    @Autowired
    public YouTubeDataServiceImpl(@Value("${youtube.api.key:}") String apiKey) {
        this(apiKey, new RestTemplate());
    }

    YouTubeDataServiceImpl(String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
    }

    @Override
    public CreatorProfile.PublicStats fetchPublicStatistics(String youtubeHandle) {
        if (youtubeHandle == null || youtubeHandle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube handle must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "YouTube Data API is not configured"
            );
        }

        String handle = youtubeHandle.trim();
        if (handle.startsWith("@")) {
            handle = handle.substring(1);
        }

        URI requestUri = UriComponentsBuilder.fromUriString(CHANNELS_API_URL)
                .queryParam("part", "statistics")
                .queryParam("forHandle", handle)
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        YouTubeChannelsResponse response;
        try {
            response = restTemplate.getForObject(requestUri, YouTubeChannelsResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "YouTube Data API request failed"
            );
        }

        List<YouTubeChannel> channels = response == null ? null : response.getItems();
        if (channels == null || channels.isEmpty() || channels.get(0).getStatistics() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "YouTube channel was not found");
        }

        YouTubeStatistics statistics = channels.get(0).getStatistics();
        try {
            Long subscriberCount = parseCount(statistics.getSubscriberCount());
            Long totalViews = parseCount(statistics.getViewCount());
            Long totalVideos = parseCount(statistics.getVideoCount());
            Double avgViews = totalViews != null && totalVideos != null && totalVideos > 0
                    ? (double) totalViews / totalVideos
                    : null;

            return CreatorProfile.PublicStats.builder()
                    .subscriberCount(subscriberCount)
                    .totalViews(totalViews)
                    .totalVideos(totalVideos)
                    .avgViews(avgViews)
                    .build();
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "YouTube Data API returned invalid channel statistics"
            );
        }
    }

    private Long parseCount(String value) {
        return value == null ? null : Long.parseLong(value);
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeChannelsResponse {
        private List<YouTubeChannel> items;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeChannel {
        private YouTubeStatistics statistics;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeStatistics {
        private String subscriberCount;
        private String viewCount;
        private String videoCount;
    }
}
