.PHONY: up run test seed build docker
up:
	docker compose up -d
run:
	mvn spring-boot:run
test:
	mvn -Dspring.profiles.active=test test
# Override: make seed PSQL_HOST=192.168.1.55 PSQL_USER=app PSQL_DB=dating
PSQL_HOST ?= localhost
PSQL_USER ?= app
PSQL_DB ?= dating
seed:
	psql -h $(PSQL_HOST) -U $(PSQL_USER) -d $(PSQL_DB) -f src/main/resources/data.sql
build:
	mvn -DskipTests package
docker:
	docker build -t dating-app:local .
