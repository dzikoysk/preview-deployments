# Preview Deployments

![Dashboard](https://cdn.discordapp.com/attachments/691990250578247710/1152625433687380120/268455881-447a2db5-5b0a-4d88-b42d-0c9c1be69480.png)

My personal kubernetes-free utility preview deployments manager for GitHub mono-repositories. 
Convenient way to manage preview deployments for smaller projects/prototypes.
Inspired by Vercel's [Preview Deployments](https://vercel.com/docs/platform/deployments#preview-deployments) integration.

```bash
$ java -jar app.jar <port=8080> <username=admin> <password=admin> <path=preview.yml>
```

**Requirements**:
* Java 17
* Nginx
* Linux

### Features

* Clones repository from GitHub _(private/public)_
* Exposes webhooks for GitHub _(push, pull_request)_
* Declarative configuration file with support for dynamic variables
* Supports multiple branches
* Supports multiple services per branch
* Start/stop handled by native commands
* Automatically generates & reloads nginx configuration

### Configuration
Assuming we have some `that-app` project with a following structure:

* `thatapp-backend` - Backend in Java, built with Gradle
* `thatapp-frontend` - Frontend app, built with NPM
* PostgreSQL

Git repository:

```bash
that-app/
├── thatapp-backend/
│   ├── gradlew
├── thatapp-frontend/
│   ├── package.json
```

The `preview.yml` configuration file should look like this:

```yaml
general:
  hostname: "preview.thatapp.com"                                # Root domain for preview deployments
  port-range: "11010-12000"                                      # Available ports for all services
  working-directory: "./that-app"                                # App working directory (useful for multiple preview deployments instances on the same machine)
  nginx-config: "/etc/nginx/sites-enabled/thatapp-preview.conf"  # Nginx's configuration file that will be managed by this instance
  git-source: "git@github.com:dzikoysk/thatapp.git"              # Sources that will be cloned
  ssh-key: "/home/dzikoysk/.ssh/github_dzikoysk_thatapp"         # Configured SSH key that will be used to clone repository
branches:
  "*": "preview"  # You can list multiple branches by name or use a wildcard (*). 
variables: # Variables that can be used in pre/services sections 
  "env-id": "id()"
  "preview-url": "url()"
  "backend-port": "port()"
  "backend-dir": "thatapp-backend"
  "frontend-port": "port()"
  "frontend-dir": "thatapp-frontend"
  "postgres-port": "port()"
pre:
  commands: # Commands that will be executed before starting services
    - "docker pull postgres:latest"
    - "chmod +x ./${backend-dir}/gradlew"
services: # List of services that will be started for each branch
  "postgres": # Service name
    start-commands: # Start the service, e.g. run docker container
      - "docker run --name postgres${env-id} -p ${postgres-port}:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres postgres -d postgres"
    stop-commands: # Stop & cleanup service
      - "docker stop postgres${env-id}"
      - "docker rm postgres${env-id}"
  "backend":
    source: "./${backend-dir}" # Path to service sources (relative to working directory)
    public: # Public URL configuration
      port: "${backend-port}"
      url: "api.${preview-url}:80"
    start-commands:
      - "./gradlew build -x integrationTest --no-daemon && java -jar build/libs/thatapp-backend-0.0.1-SNAPSHOT.jar"
    stop-commands:
      - "$exit"
    environment: # Environment variables that will be passed to service
      "SERVER_PORT": "${backend-port}"
      "SPRING_PROFILES_ACTIVE": "prod"
      "FRONTEND_URL": "${preview-url}"
      "DATABASE_URL": "jdbc:postgresql://localhost:${postgres-port}/postgres"
      "POSTGRES_DB_USER": "postgres"
      "POSTGRES_DB_PASSWORD": "postgres"
  "frontend":
    source: "./${frontend-dir}"
    public:
      port: "${frontend-port}"
      url: "${preview-url}:80"
    start-commands:
      - "npm install && npm run build && npm run start -- -p ${frontend-port}"
    stop-commands:
      - "$exit"
    environment:
      "NODE_ENV": "production"
      "API_URL": "api.${preview-url}:${backend-port}"
```

### Environment variables functions

* `id()` - Generates unique id for each preview deployment
* `url()` - Returns preview deployment url
* `port()` - Returns available port for service (each call returns different port)

### Notes

Increase `server_names_hash` in `/etc/nginx/nginx.conf` to properly handle long server names:

```
server_names_hash_bucket_size  256;
```
