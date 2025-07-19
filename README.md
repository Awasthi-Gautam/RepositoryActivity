# Repository Activity Tracker

A Spring Boot WebFlux service that fetches all public repository and commit activity for GitHub (and other VCS systems via a pluggable connector SPI) for a given username.

---

## Features

- **GitHub connector**: fetch all public repos for a user, then retrieve the most recent commits.  
- **Bitbucket connector** (plug‑and‑play): swap in a new connector implementation without touching controllers.  
- **Rate‑limit handling**: automatically waits on 429 responses until the GitHub reset time.  
- **Permission skipping**: skips 403 pages when you don’t have access to a repo.  
- **Global error handler**: consistent JSON error payloads (`502`, `404`, `500`, etc.).

---

## Prerequisites

- **Java 11** or higher  
- **Gradle** (or use the included Gradle wrapper)  
- A **GitHub Personal Access Token** with `repo` scope  

---

## Assumptions

- User has Personal token. Paste it in application.properties before Testing
- The api works for a github user only and can be extended to fetch repositories for Organization.
- Rate limiting is implemented but not tested.

---

# FOR TESTING 
Clone the Repository

## Configuration 

Update GITHUB_TOKEN in `application.properties` (under `src/main/resources`) with Personal token


## **Building & Running**
 Run the following commands
- `chmod +x ./gradlew`
- `./gradlew clean bootRun`


## **REST API**
1. Health‑check 
- `curl -X GET "http://localhost:8080/api/v1/activity/health"`
 The Response should be "SERVICE RUNNING"
2. Fetch Repositories and most recent 20 commits,
   - `GET /api/v1/activity/{**source**}/repos/{**owner**}`
   - Path parameters:
     - **source**: "github" or "bitbucket"
     - **owner**: GitHub username (e.g. Awasthi-Gautam/octocat) or Bitbucket workspace

  - Use the below curl for getting response.

    ```
    curl -X GET "http://localhost:8080/api/v1/activity/github/repos/Awasthi-Gautam" \
     -H "Accept: application/json"
    
    
    RESPONSE -
    [{
    "repositoryName": "RepositoryActivity",
    "recentCommits": [
      {
        "sha": "0a2ace69e0534b3fee5d35fb81ff5b6874c33bad",
        "message": "Initial commit for repository tracking",
        "authorName": "Gautam Awasthi",
        "authorEmail": "gautamawasthi@Mac.lan",
        "timestamp": "2025-07-19T07:57:59Z"
      },
      {
        "sha": "56b474cbd40f6a9939530c9616d5fb0bebb70ec8",
        "message": "Initial commit",
        "authorName": "Awasthi-Gautam",
        "authorEmail": "awasthigautam511@gmail.com",
        "timestamp": "2025-07-19T08:02:41Z"
      }
    ] },{
    "repositoryName": "RepoTracker",
    "recentCommits": [
      {
        "sha": "2ef98c7a65624d1dd77aa6fbe9a0607747353cc1",
        "message": "Initial commit",
        "authorName": "Awasthi-Gautam",
        "authorEmail": "awasthigautam511@gmail.com",
        "timestamp": "2025-07-18T09:28:07Z"
      }
    ]}]

# Extending to New VCS
- Implement RepositoryActivityConnector in a new @Service (e.g. BitBucketConnector/GitLabConnector).

- Register your connector in ConnectorFactory.

- Hit /api/v1/activity/gitlab/repos/{owner} — no controller changes needed.


Reach out at Awasthigautam511@gmail.com for any issues/enhancements.