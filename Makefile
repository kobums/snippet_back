tag=latest

all: server

server: dummy
	./gradlew clean build -x test

run:
	./gradlew bootRun

test: dummy
	./gradlew test

dockerbuild:
	docker buildx build --platform linux/amd64 -t kobums/snippetback:$(tag) .

docker: server dockerbuild

dockerrun:
	docker run --platform linux/amd64 -d --name="snippetback" -p 8008:8008 kobums/snippetback:$(tag)

push: docker
	docker push kobums/snippetback:$(tag)

compose-up:
	docker-compose up -d --build

compose-down:
	docker-compose down

compose-logs:
	docker-compose logs -f

clean:
	./gradlew clean
	docker stop snippetback || true
	docker rm snippetback || true

dummy: