ifeq ($(OS),Windows_NT)
	MVN := ./mvnw.cmd
	SETUP_DB := powershell -ExecutionPolicy Bypass -File ./scripts/setup-db.ps1 -PostgresPassword 2518
else
	SHELL := /bin/bash
	MVN := ./mvnw
	SETUP_DB := ./scripts/setup-db.sh
endif

.PHONY: setup-db backend frontend test build-frontend

setup-db:
	$(SETUP_DB)

backend:
	$(MVN) spring-boot:run

frontend:
	cd frontend-vue && npm install && npm run dev -- --host 127.0.0.1

test:
	$(MVN) test

build-frontend:
	cd frontend-vue && npm install && npm run build
