package com.repository.tracker.RepoTracker.pojo;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@Builder
public class RepositoryActivity {
    String repositoryName;
    List<RepositoryCommit> recentCommits;
}


