package com.repository.tracker.RepoTracker.connector.github;

import com.repository.tracker.RepoTracker.connector.RepositoryActivityConnector;
import com.repository.tracker.RepoTracker.exceptions.ConnectorException;
import com.repository.tracker.RepoTracker.pojo.RepositoryActivity;
import com.repository.tracker.RepoTracker.pojo.github.GitHubCommitDto;
import com.repository.tracker.RepoTracker.pojo.github.GithubRepositoryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GitHubConnector implements RepositoryActivityConnector {

    private static final int REPOS_PER_PAGE = 20;
    private static final int COMMITS_PER_PAGE = 30;
    private static final int REQUIRED_COMMITS = 20;

    private static final String PATH_REPOS = "/users/{user}/repos";
    private static final String PATH_COMMITS = "/repos/{owner}/{repo}/commits";

    private static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    private final WebClient githubWebClient;

    public GitHubConnector(@Qualifier("gitHubWebClient") WebClient githubWebClient) {
        this.githubWebClient = githubWebClient;
    }


    @Override
    public List<RepositoryActivity> getActivities(String userName) throws ConnectorException {
        try {
            List<GithubRepositoryDto> repos = fetchAllRepos(userName);
            List<RepositoryActivity> activities = new ArrayList<>();

            for (GithubRepositoryDto repo : repos) {
                List<GitHubCommitDto> commits = fetchRecentCommits(userName, repo.getName());
                activities.add(toRepositoryActivity(repo, commits));
            }

            return activities;
        } catch (Exception e) {
            throw new ConnectorException("Error fetching GitHub activity for " + userName, e);
        }
    }

    private List<GitHubCommitDto> fetchRecentCommits(String owner, String repoName) throws ConnectorException {
        List<GitHubCommitDto> commits = new ArrayList<>();
        int page = 1;

        while (true) {
            final int currentPage = page;

            // 1) Perform the request and get a ResponseEntity<List<Dto>>
            ClientResponse response = githubWebClient.get()
                    .uri(uri -> uri.path(PATH_COMMITS)
                            .queryParam("per_page", COMMITS_PER_PAGE)
                            .queryParam("page", currentPage)
                            .build(owner, repoName))
                    .exchange()
                    .block();

            if (response == null) {
                throw new ConnectorException("Null response for commits page " + currentPage, null);
            }

            List<GitHubCommitDto> pageItems = handleResponse(response, GitHubCommitDto.class, "commits page " + page);

            // stop if no more commits
            if (pageItems.isEmpty()) {
                break;
            }

            // collect
            commits.addAll(pageItems);

            // stop if we've gathered enough
            if (commits.size() >= REQUIRED_COMMITS) {
                break;
            }

            page++;
        }

        // trim to exactly REQUIRED_COMMITS if we over‑fetched
        if (commits.size() > REQUIRED_COMMITS) {
            return commits.subList(0, REQUIRED_COMMITS);
        }
        return commits;
    }

    private RepositoryActivity toRepositoryActivity(GithubRepositoryDto repo, List<GitHubCommitDto> commits) {
        return RepositoryActivity.builder().repositoryName(repo.getName())
                .recentCommits(commits.stream().map(GitHubCommitDto::toCommit).collect(Collectors.toList())).build();
    }

    private List<GithubRepositoryDto> fetchAllRepos(String userName) throws ConnectorException {
        List<GithubRepositoryDto> all = new ArrayList<>();
        int page = 1;
        while (true) {
            int finalPage = page;
            ClientResponse response = githubWebClient.get()
                    .uri(uri -> uri
                            .path(PATH_REPOS)
                            .queryParam("per_page", REPOS_PER_PAGE)
                            .queryParam("page", finalPage)
                            .queryParam("type", "all")
                            .build(userName))
                    .exchange()
                    .block();
            ;

            if (response == null) {
                throw new ConnectorException("Null response for repos page " + finalPage, null);
            }

            List<GithubRepositoryDto> pageItems = handleResponse(response, GithubRepositoryDto.class,
                    "Repos page " + page);

            if (pageItems.isEmpty()) {
                break;
            }

            all.addAll(pageItems);
            page++;
        }

        return all;
    }

    private <T> List<T> handleResponse(ClientResponse resp, Class<T> dtoClass, String context)
            throws ConnectorException {
        HttpStatus status = resp.statusCode();

        if (status.is2xxSuccessful()) {
            return resp.bodyToFlux(dtoClass).collectList().blockOptional().orElse(Collections.emptyList());
        }

        if (status == HttpStatus.FORBIDDEN ||
                status == HttpStatus.NOT_FOUND ||
                status == HttpStatus.CONFLICT) {
            log.warn("Skipping {} (status {})", context, status);
            return Collections.emptyList();
        }

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            return handleRateLimit(resp, dtoClass, context);
        }

        throw new ConnectorException("Unexpected status " + status + " during " + context, null);
    }

    private <T> List<T> handleRateLimit(ClientResponse resp, Class<T> dtoClass, String context)
            throws ConnectorException {
        String resetHeader = resp.headers().header(HEADER_RATE_LIMIT_RESET)
                .stream().findFirst().orElse(null);
        if (resetHeader != null) {
            long waitSecs = Long.parseLong(resetHeader)
                    - Instant.now().getEpochSecond()
                    + 1;
            log.warn("Rate limited during {}: sleeping {}s", context, waitSecs);
            if (waitSecs > 0) {
                try {
                    Thread.sleep(waitSecs * 1_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ConnectorException("Interrupted waiting for rate limit reset", e);
                }
            }

        }
        throw new ConnectorException("Rate limit exceeded during " + context, null);
    }
}


