ifeq ($(OS),Windows_NT)
	MVN := ./mvnw.cmd
	SETUP_DB := powershell -ExecutionPolicy Bypass -File ./scripts/setup-db.ps1 -PostgresPassword 2518
	BACKEND := cmd /C "set DB_USERNAME=ksu&& set DB_PASSWORD=ksu&& .\mvnw.cmd spring-boot:run"
else
	SHELL := /bin/bash
	MVN := ./mvnw
	SETUP_DB := ./scripts/setup-db.sh
	BACKEND := DB_USERNAME=ksu DB_PASSWORD=ksu $(MVN) spring-boot:run
endif

.PHONY: setup-db backend frontend test build-frontend

setup-db:
	$(SETUP_DB)

backend:
	$(BACKEND)

frontend:
	cd frontend-vue && npm install && npm run dev -- --host 127.0.0.1

test:
	$(MVN) test

build-frontend:
	cd frontend-vue && npm install && npm run build
