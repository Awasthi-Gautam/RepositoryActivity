package com.repository.tracker.RepoTracker.connector.github;

import com.repository.tracker.RepoTracker.connector.RepositoryActivityConnector;
import com.repository.tracker.RepoTracker.exceptions.ConnectorException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import com.repository.tracker.RepoTracker.pojo.github.GithubRepositoryDto;
import com.repository.tracker.RepoTracker.pojo.RepositoryActivity;
import com.repository.tracker.RepoTracker.pojo.github.GitHubCommitDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GitHubConnector implements RepositoryActivityConnector {

    private static final Integer REPOS_PER_PAGE = 20;
    private static final int REQUIRED_COMMITS = 20;
    private static final Object COMMITS_PER_PAGE = 30;
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
                    .uri(uri -> uri
                            .path("/repos/{owner}/{repo}/commits")
                            .queryParam("per_page", COMMITS_PER_PAGE)
                            .queryParam("page",    currentPage)
                            .build(owner, repoName))
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .exchange()
                    .block();

            if (response == null) {
                throw new ConnectorException("Null response for commits page " + currentPage, null);
            }

            HttpStatus status = response.statusCode();
            List<GitHubCommitDto> pageItems;

            System.out.println("Status: " + status);
            if (status.is2xxSuccessful()) {
                pageItems = response.bodyToFlux(GitHubCommitDto.class)
                        .collectList()
                        .block();
                if (pageItems == null) pageItems = Collections.emptyList();

            } else if (status == HttpStatus.FORBIDDEN || status == HttpStatus.CONFLICT || status == HttpStatus.NOT_FOUND) {
                // skip pages for which we don't have permission
                System.out.println("Skipping commits page " + currentPage + " (403 Forbidden)");
                pageItems = Collections.emptyList();

            } else if (status == HttpStatus.TOO_MANY_REQUESTS) {
                // rate‑limit: wait until reset then retry
                String reset = response.headers().header("X-RateLimit-Reset").stream().findFirst().orElse(null);
                if (reset != null) {
                    long waitSecs = Long.parseLong(reset)
                            - Instant.now().getEpochSecond()
                            + 1;
                    if (waitSecs > 0) {
                        System.out.println("Rate limit hit: sleeping " + waitSecs + "s before retrying commits page " + currentPage);
                        try {
                            Thread.sleep(waitSecs * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ConnectorException("Interrupted waiting for rate limit reset", ie);
                        }
                    }
                    // retry same page
                    continue;
                } else {
                    throw new ConnectorException("429 Too Many Requests but no X-RateLimit-Reset header", null);
                }

            } else {
                // unexpected error
                throw new ConnectorException(
                        "Unexpected status " + status + " on commits page " + currentPage,
                        null
                );
            }

            // 2) stop if no more commits
            if (pageItems.isEmpty()) {
                break;
            }

            // 3) collect
            commits.addAll(pageItems);

            // 4) stop if we've gathered enough
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


//    private List<GitHubCommitDto> fetchRecentCommits(String owner, String repoName) {
//        List<GitHubCommitDto> commits = new ArrayList<>();
//        int page = 1;
//
//        while (true) {
//            final int currentPage = page;
//
//            List<GitHubCommitDto> pageItems = githubWebClient.get()
//                    .uri(uri -> uri
//                            .path("/repos/{owner}/{repo}/commits")
//                            .queryParam("per_page", COMMITS_PER_PAGE)
//                            .queryParam("page", currentPage)
//                            .build(owner, repoName))
//                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
//                    .retrieve()
//                    .bodyToFlux(GitHubCommitDto.class)
//                    .collectList()
//                    .block();
//
//            // stop if no more commits
//            if (pageItems == null || pageItems.isEmpty()) {
//                break;
//            }
//
//            commits.addAll(pageItems);
//
//            // if we've got enough, stop
//            if (commits.size() >= REQUIRED_COMMITS) {
//                break;
//            }
//
//            page++;
//
//        }
//        // trim to exactly REQUIRED_COMMITS if we over‑fetched
//        if (commits.size() > REQUIRED_COMMITS) {
//            return commits.subList(0, REQUIRED_COMMITS);
//        }
//        return commits;
//    }

    private RepositoryActivity toRepositoryActivity(GithubRepositoryDto repo, List<GitHubCommitDto> commits) {
        return RepositoryActivity.builder().repoName(repo.getName())
                .recentCommits(commits.stream().map(GitHubCommitDto::toCommit).collect(Collectors.toList())).build();
    }

    private List<GithubRepositoryDto> fetchAllRepos(String userName) throws ConnectorException {
        List<GithubRepositoryDto> all = new ArrayList<>();
        int page = 1;
        while (true) {
            int finalPage = page;
            ResponseEntity<List<GithubRepositoryDto>> response = githubWebClient.get()
                    .uri(uri -> uri
                            .path("/users/{user}/repos")
                            .queryParam("per_page", REPOS_PER_PAGE)
                            .queryParam("page", finalPage)
                            .queryParam("type",    "all")
                            .build(userName))
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .toEntityList(GithubRepositoryDto.class)
                    .block();

            if (response == null) {
                throw new ConnectorException("Null response for repos page " + finalPage, null);
            }

            HttpStatus status = response.getStatusCode();
            List<GithubRepositoryDto> pageItems;

            if (status.is2xxSuccessful()) {
                pageItems = response.getBody();
                if (pageItems == null) pageItems = Collections.emptyList();

            } else if (status == HttpStatus.FORBIDDEN) {
                System.out.println("Skipping page " + finalPage + " (403 Forbidden)");
                pageItems = Collections.emptyList();

            }
            else if (status == HttpStatus.TOO_MANY_REQUESTS) {
                // rate‑limit logic
                String reset = response.getHeaders()
                        .getFirst("X-RateLimit-Reset");
                if (reset != null) {
                    long waitSecs = Long.parseLong(reset)
                            - Instant.now().getEpochSecond()
                            + 1;
                    if (waitSecs > 0) {
                        System.out.println("Rate limit hit: sleeping " + waitSecs + "s");
                        try {
                            Thread.sleep(waitSecs * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new ConnectorException("Interrupted waiting for rate reset", ie);
                        }
                    }
                    // retry same page
                    continue;
                } else {
                    throw new ConnectorException("429 Too Many Requests (no reset header)", null);
                }

            }
            else {
                throw new ConnectorException(
                        "Unexpected status " + status + " on repos page " + finalPage,
                        null
                );
            }

            // 2) Stop if no items
            if (pageItems.isEmpty()) {
                break;
            }

            all.addAll(pageItems);
            page++;
        }

        return all;
    }

//    private List<GithubRepositoryDto> fetchAllRepos(String userName) {
//        List<GithubRepositoryDto> all = new ArrayList<>();
//        int page = 1;
//        while (true) {
//            int finalPage = page;
//            List<GithubRepositoryDto> pageItems = githubWebClient.get()
//                    .uri(uri -> uri
//                            .path("/users/{userName}/repos")
//                            .queryParam("per_page", REPOS_PER_PAGE)
//                            .queryParam("page", finalPage)
////                            .queryParam("type", "all")
//                            .build(userName))
//                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
//                    .retrieve()
//                    .bodyToFlux(GithubRepositoryDto.class)
//                    .collectList()
//                    .block();
//
//            if (pageItems == null || pageItems.isEmpty()) {
//                break;
//            }
//            System.out.println("repos page wise: " + pageItems.stream().map(GithubRepositoryDto::getFullName).collect(Collectors.joining(",")));
//            all.addAll(pageItems);
//            page++;
//        }
//
//        return all;
//    }
}


