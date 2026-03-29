.PHONY: up run test seed build docker
up:
	docker compose up -d
run:
	mvn spring-boot:run
test:
	mvn -Dspring.profiles.active=test test
seed:
	psql -h localhost -U app -d dating -f src/main/resources/data.sql
build:
	mvn -DskipTests package
docker:
	docker build -t dating-app:local .
