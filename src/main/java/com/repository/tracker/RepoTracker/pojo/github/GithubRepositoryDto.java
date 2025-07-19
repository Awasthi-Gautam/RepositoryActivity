package com.repository.tracker.RepoTracker.pojo.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class GithubRepositoryDto {
    @JsonProperty("name")
    private String name;

    @JsonProperty("full_name")
    private String fullName;
}
