package com.repository.tracker.RepoTracker.pojo.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import com.repository.tracker.RepoTracker.pojo.RepositoryCommit;

import java.time.Instant;

/**
 * https://docs.github.com/en/rest/commits/commits?apiVersion=2022-11-28#list-commits
 * */
@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class GitHubCommitDto {
    private String sha;

    /** The nested “commit” object in GitHub’s JSON */
    @JsonProperty("commit")
    private InnerCommit commit;

    @Data
    @Getter
    @Setter
    public static class InnerCommit {
        /** The commit message */
        private String message;
        /** The author info inside the commit object */
        private Author author;
    }

    @Data
    @Getter
    @Setter
    public static class Author {
        private String name;
        private String email;
        /** The commit timestamp (e.g. "2025-07-18T12:34:56Z") */
        @JsonProperty("date")
        private Instant timestamp;
    }

    public RepositoryCommit toCommit() {
        Author a = this.getCommit().getAuthor();
        return new RepositoryCommit(
                this.getSha(),
                this.getCommit().getMessage(),
                a.getName(),
                a.getEmail(),
                a.getTimestamp()
        );
    }
}
