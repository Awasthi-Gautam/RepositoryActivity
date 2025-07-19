package com.repository.tracker.RepoTracker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public WebClient gitHubWebClient(@Value("${github.api.url}") String githubApiUrl) {
        return WebClient.builder().baseUrl(githubApiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .build();
    }

}
