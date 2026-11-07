up:
	docker compose up -d

run:
	./gradlew bootRun

test:
	./gradlew test

api-test:
	node integration-test/api-test.js
