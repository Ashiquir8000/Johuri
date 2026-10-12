package com.example.johuri.service;

import com.example.johuri.DTO.YouTubeTelemetryDTO;
import com.example.johuri.entity.CreatorProfile;

public interface YouTubeDataService {
    CreatorProfile.PublicStats fetchPublicStatistics(String youtubeHandle);
    YouTubeTelemetryDTO fetchCreatorTelemetry(String handle);
}
