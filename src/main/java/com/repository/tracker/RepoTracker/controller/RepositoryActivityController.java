package com.repository.tracker.RepoTracker.controller;

import com.repository.tracker.RepoTracker.connector.ConnectorFactory;
import com.repository.tracker.RepoTracker.connector.RepositoryActivityConnector;
import com.repository.tracker.RepoTracker.exceptions.ConnectorException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.repository.tracker.RepoTracker.pojo.RepositoryActivity;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activity")
public class RepositoryActivityController {
    private final ConnectorFactory factory;

    public RepositoryActivityController(ConnectorFactory factory) {
        this.factory = factory;
    }

    /**
     * GET /api/v1/activity/{source}/repos/{owner}
     * e.g. /api/v1/activity/github/repos/octocat
     *      /api/v1/activity/bitbucket/repos/myteam
     */
    @GetMapping(value = "/{source}/repos/{owner}")
    public Mono<List<RepositoryActivity>> getActivity(
            @PathVariable String source,
            @PathVariable String owner
    ) throws ConnectorException {
        System.out.println("source: " + source);
        RepositoryActivityConnector connector = factory.get(source);
        if (connector == null) {
            throw new ConnectorException("Unknown source: " + source, null);
        }
        return Mono.fromCallable(() -> connector.getActivities(owner))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping( "/health")
    public String getServiceStatus() {
       return "SERVICE RUNNING";
    }
}
