# fox-intel-demo

## Quickstart (local development)

Running a local development environment is the easiest way to get things up and running quickly. This involves:

* Running `docker-compose` to bring up Kafka and Postgres.
* Running Lagom's local development environment.
* Running the Cloudflow application with the local sandbox.

### Kafka & Postgres

In the `/docker` directory you'll find a `docker-compose.yml` which will run Zookeeper, Kafka and Postgres. The Postgres container is configured to run from an image built locally which will configure databases for the Lagom services when it's first created.

```
cd docker
docker-compose up
```

### Lagom services

Running the Lagom services is straightforward with Lagom's SBT plugin. The default configuration for the services points to Kafka and Postgres running locally (from the previous step).

```
cd lagom
sbt runAll
```

### Cloudflow application

Cloudflow provides a local sandbox experience that helps developers confirm the behaviour of an application without having to build, publish and deploy an application to a full Kubernetes cluster.

```
cd cloudflow
sbt runLocal
```

#### Monitoring Locally

* Lagom will output logs to the local console
* The Cloudflow sandbox will output the location of the `tmp` file being used to aggregate logs from all the streamlets - just `tail -f` that file to view the logs.

## Deploying to Kubernetes

[COMING SOON]
