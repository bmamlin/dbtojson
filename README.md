# DbToJson

Generates OpenMRS database schema in JSON format.

## For OpenMRS Platform

```bash
OPENMRS_DISTRO=openmrs-distro-platform OPENMRS_DISTRO_VERSION=2.3.1 docker-compose up -d
docker-compose logs -f dbtojson
```

Cleanup:

```bash
docker-compose down -v
```

## For OpenMRS Reference Application

```bash
OPENMRS_DISTRO=openmrs-reference-application-distro OPENMRS_DISTRO_VERSION=2.5 docker-compose up -d
docker-compose logs -f dbtojson
```

Cleanup:

```bash
docker-compose down -v
```
