package com.example.johuri;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.johuri.repository.CreatorProfileRepository;
import com.example.johuri.repository.DealRepository;
import com.example.johuri.repository.DigitalProductRepository;
import com.example.johuri.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class JohuriApplicationTests {

    @MockitoBean
    private CreatorProfileRepository creatorProfileRepository;
    @MockitoBean
    private DealRepository dealRepository;
    @MockitoBean
    private DigitalProductRepository digitalProductRepository;
    @MockitoBean
    private UserRepository userRepository;

    @Test
    void contextLoads() {
    }

}
