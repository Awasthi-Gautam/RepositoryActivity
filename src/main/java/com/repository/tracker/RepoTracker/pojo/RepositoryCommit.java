package com.repository.tracker.RepoTracker.pojo;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@Getter
@Setter
@AllArgsConstructor
public class RepositoryCommit {
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private Instant timestamp;
}
