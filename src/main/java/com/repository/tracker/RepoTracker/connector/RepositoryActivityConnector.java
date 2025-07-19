package com.repository.tracker.RepoTracker.connector;

import com.repository.tracker.RepoTracker.exceptions.ConnectorException;
import com.repository.tracker.RepoTracker.pojo.RepositoryActivity;

import java.util.List;

public interface RepositoryActivityConnector {

    List<RepositoryActivity> getActivities(String owner) throws ConnectorException;
}
