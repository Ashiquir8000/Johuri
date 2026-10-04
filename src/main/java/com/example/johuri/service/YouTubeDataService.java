package com.example.johuri.service;

import com.example.johuri.entity.CreatorProfile;

public interface YouTubeDataService {

    CreatorProfile.PublicStats fetchPublicStatistics(String youtubeHandle);
}
