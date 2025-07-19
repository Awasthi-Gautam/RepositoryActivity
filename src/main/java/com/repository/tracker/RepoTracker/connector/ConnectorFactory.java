package com.repository.tracker.RepoTracker.connector;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ConnectorFactory {
    private final Map<String, RepositoryActivityConnector> connectors;

    public ConnectorFactory(
            @Qualifier("gitHubConnector") RepositoryActivityConnector gh
    ) {
        this.connectors = Map.of(
                "github", gh
        );
    }

    public RepositoryActivityConnector get(String type) {
        return connectors.get(type.toLowerCase());
    }
}
