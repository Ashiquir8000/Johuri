package com.example.johuri.service;

import com.example.johuri.DTO.YouTubeTelemetryDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class YouTubeDataServiceImpl implements YouTubeDataService {

    private static final String CHANNELS_API_URL = "https://www.googleapis.com/youtube/v3/channels";
    private static final String SEARCH_API_URL = "https://www.googleapis.com/youtube/v3/search";

    private final String apiKey;
    private final RestTemplate restTemplate;

    @Autowired
    public YouTubeDataServiceImpl(@Value("${youtube.api.key:}") String apiKey) {
        this(apiKey, new RestTemplate());
    }

    YouTubeDataServiceImpl(String apiKey, RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        String maskedKey = (apiKey != null && apiKey.length() > 6) 
            ? apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4) 
            : "NOT_SET";
        System.out.println("Loaded YouTube API Key: " + maskedKey);
    }

    @Override
    public CreatorProfile.PublicStats fetchPublicStatistics(String youtubeHandle) {
        YouTubeTelemetryDTO telemetry = fetchCreatorTelemetry(youtubeHandle);
        return CreatorProfile.PublicStats.builder()
                .subscriberCount(telemetry.getSubscriberCount())
                .totalViews(telemetry.getViewCount())
                .totalVideos(telemetry.getVideoCount())
                .avgViews(telemetry.getViewCount() != null && telemetry.getVideoCount() != null && telemetry.getVideoCount() > 0 
                        ? (double) telemetry.getViewCount() / telemetry.getVideoCount() : null)
                .build();
    }

    @Override
    public YouTubeTelemetryDTO fetchCreatorTelemetry(String youtubeHandle) {
        if (youtubeHandle == null || youtubeHandle.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube handle must not be blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "YouTube Data API is not configured");
        }

        String decodedHandle = java.net.URLDecoder.decode(youtubeHandle, java.nio.charset.StandardCharsets.UTF_8).trim();
        if (decodedHandle.startsWith("@")) {
            decodedHandle = decodedHandle.substring(1);
        }
        String cleanHandle = decodedHandle;

        URI channelUri = UriComponentsBuilder.fromUriString(CHANNELS_API_URL)
                .queryParam("part", "snippet,statistics,brandingSettings")
                .queryParam("forHandle", cleanHandle)
                .queryParam("key", apiKey)
                .build().encode().toUri();

        YouTubeChannelsResponse response = null;
        try {
            response = restTemplate.getForObject(channelUri, YouTubeChannelsResponse.class);
        } catch (RestClientException ignored) {
        }
        
        List<YouTubeChannel> channels = response == null ? null : response.getItems();
        
        if (channels == null || channels.isEmpty()) {
            URI searchUri = UriComponentsBuilder.fromUriString(SEARCH_API_URL)
                    .queryParam("part", "snippet")
                    .queryParam("type", "channel")
                    .queryParam("q", cleanHandle)
                    .queryParam("maxResults", 3)
                    .queryParam("key", apiKey)
                    .build().encode().toUri();
            try {
                YouTubeSearchResponse searchResponse = restTemplate.getForObject(searchUri, YouTubeSearchResponse.class);
                if (searchResponse != null && searchResponse.getItems() != null && !searchResponse.getItems().isEmpty()) {
                    String bestChannelId = null;
                    long maxSubs = -1;
                    for (YouTubeSearchResult sr : searchResponse.getItems()) {
                        String cid = sr.getId() != null ? sr.getId().getChannelId() : null;
                        if (cid != null) {
                            URI statsUri = UriComponentsBuilder.fromUriString(CHANNELS_API_URL)
                                    .queryParam("part", "statistics")
                                    .queryParam("id", cid)
                                    .queryParam("key", apiKey)
                                    .build().encode().toUri();
                            try {
                                YouTubeChannelsResponse statsRes = restTemplate.getForObject(statsUri, YouTubeChannelsResponse.class);
                                if (statsRes != null && statsRes.getItems() != null && !statsRes.getItems().isEmpty()) {
                                    YouTubeChannel ch = statsRes.getItems().get(0);
                                    YouTubeStatistics st = ch.getStatistics();
                                    long subs = parseCount(st != null ? st.getSubscriberCount() : null);
                                    
                                    String cUrl = ch.getSnippet() != null ? ch.getSnippet().getCustomUrl() : "";
                                    if (cUrl != null && (cUrl.equalsIgnoreCase(cleanHandle) || cUrl.equalsIgnoreCase("@" + cleanHandle))) {
                                        subs += 100_000_000_000L; // Guarantee selection
                                    }
                                    
                                    if (subs > maxSubs) {
                                        maxSubs = subs;
                                        bestChannelId = cid;
                                    }
                                }
                            } catch (RestClientException ignored) {}
                        }
                    }
                    if (bestChannelId != null) {
                        URI finalChannelUri = UriComponentsBuilder.fromUriString(CHANNELS_API_URL)
                                .queryParam("part", "snippet,statistics,brandingSettings")
                                .queryParam("id", bestChannelId)
                                .queryParam("key", apiKey)
                                .build().encode().toUri();
                        response = restTemplate.getForObject(finalChannelUri, YouTubeChannelsResponse.class);
                        channels = response == null ? null : response.getItems();
                    }
                }
            } catch (RestClientException ignored) {}
        }
        
        if (channels == null || channels.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "YouTube channel was not found after fallback search");
        }

        YouTubeChannel channel = channels.get(0);
        YouTubeStatistics stats = channel.getStatistics();
        YouTubeSnippet snippet = channel.getSnippet();

        String bestAvatarUrl = null;
        if (snippet != null && snippet.getThumbnails() != null) {
            YouTubeThumbnails thumbs = snippet.getThumbnails();
            if (thumbs.getHigh() != null && thumbs.getHigh().getUrl() != null) {
                bestAvatarUrl = thumbs.getHigh().getUrl();
            } else if (thumbs.getMedium() != null && thumbs.getMedium().getUrl() != null) {
                bestAvatarUrl = thumbs.getMedium().getUrl();
            } else if (thumbs.getDefaultThumb() != null && thumbs.getDefaultThumb().getUrl() != null) {
                bestAvatarUrl = thumbs.getDefaultThumb().getUrl();
            }
        }

        YouTubeTelemetryDTO telemetry = YouTubeTelemetryDTO.builder()
                .channelId(channel.getId())
                .handle("@" + cleanHandle)
                .title(snippet != null ? snippet.getTitle() : null)
                .description(snippet != null ? snippet.getDescription() : null)
                .avatarUrl(bestAvatarUrl)
                .subscriberCount(stats != null ? parseCount(stats.getSubscriberCount()) : 0L)
                .viewCount(stats != null ? parseCount(stats.getViewCount()) : 0L)
                .videoCount(stats != null ? parseCount(stats.getVideoCount()) : 0L)
                .build();

        // Fetch top videos
        URI searchVideosUri = UriComponentsBuilder.fromUriString(SEARCH_API_URL)
                .queryParam("part", "snippet")
                .queryParam("channelId", channel.getId())
                .queryParam("order", "viewCount")
                .queryParam("type", "video")
                .queryParam("maxResults", 5)
                .queryParam("key", apiKey)
                .build().encode().toUri();

        try {
            YouTubeSearchResponse searchResponse = restTemplate.getForObject(searchVideosUri, YouTubeSearchResponse.class);
            if (searchResponse != null && searchResponse.getItems() != null) {
                List<YouTubeTelemetryDTO.TopVideoDTO> topVideos = searchResponse.getItems().stream()
                        .map(item -> YouTubeTelemetryDTO.TopVideoDTO.builder()
                                .videoId(item.getId().getVideoId())
                                .title(item.getSnippet().getTitle())
                                .thumbnailUrl(item.getSnippet().getThumbnails() != null && item.getSnippet().getThumbnails().getHigh() != null ? item.getSnippet().getThumbnails().getHigh().getUrl() : null)
                                .build())
                        .collect(Collectors.toList());
                telemetry.setTopVideos(topVideos);
            } else {
                telemetry.setTopVideos(new ArrayList<>());
            }
        } catch (RestClientException e) {
            telemetry.setTopVideos(new ArrayList<>());
        }

        return telemetry;
    }

    private Long parseCount(String value) {
        if (value == null || value.isBlank()) return 0L;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeChannelsResponse {
        private List<YouTubeChannel> items;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeChannel {
        private String id;
        private YouTubeSnippet snippet;
        private YouTubeStatistics statistics;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeSnippet {
        private String title;
        private String description;
        private String customUrl;
        private YouTubeThumbnails thumbnails;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeThumbnails {
        private YouTubeThumbnail high;
        private YouTubeThumbnail medium;
        @com.fasterxml.jackson.annotation.JsonProperty("default")
        private YouTubeThumbnail defaultThumb;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeThumbnail {
        private String url;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeStatistics {
        private String subscriberCount;
        private String viewCount;
        private String videoCount;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeSearchResponse {
        private List<YouTubeSearchResult> items;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeSearchResult {
        private YouTubeSearchId id;
        private YouTubeSnippet snippet;
    }

    @Data
    @NoArgsConstructor
    private static class YouTubeSearchId {
        private String videoId;
        private String channelId;
    }
}
