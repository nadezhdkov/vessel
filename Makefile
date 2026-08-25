# Vessel — convenience wrapper around ./gradlew.
#
# Every target here just shells out to Gradle; nothing here is load-bearing
# build logic, it only exists to save typing. See CLAUDE.md/README.md for
# what each command actually does under the hood.
#
# Usage: make <target>            (or just `make` for this list)
#        make benchmark BEANS=2000

GRADLE := ./gradlew
BEANS  ?= 500

.DEFAULT_GOAL := help

.PHONY: help build clean test \
        test-core test-http test-web test-config test-app test-examples \
        run \
        benchmark benchmark-vessel benchmark-spring \
        publish-local sign-check publish \
        format-check

help: ## Show this list of targets
	@echo "Vessel — available targets:"
	@echo
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo
	@echo "Variables:"
	@echo "  BEANS=N   bean count for the benchmark targets (default: 500)"

build: ## Full build — every module, all tests, in the main multi-module build
	$(GRADLE) build

clean: ## Remove build output from every module (main build only, not benchmarks/)
	$(GRADLE) clean

test: ## Run every module's tests (equivalent to `make build` without assembling jars)
	$(GRADLE) test

test-core: ## Run only vessel-core's tests
	$(GRADLE) :vessel-core:test

test-http: ## Run only vessel-http's tests
	$(GRADLE) :vessel-http:test

test-web: ## Run only vessel-web's tests
	$(GRADLE) :vessel-web:test

test-config: ## Run only vessel-config's tests
	$(GRADLE) :vessel-config:test

test-app: ## Run only vessel-app's tests
	$(GRADLE) :vessel-app:test

test-examples: ## Run only vessel-examples' smoke test
	$(GRADLE) :vessel-examples:test

run: ## Start the example app on http://localhost:8080 (Ctrl+C to stop)
	$(GRADLE) :vessel-examples:run

benchmark: benchmark-vessel benchmark-spring ## Run both startup benchmarks (Vessel vs. Spring), BEANS=N to size it

benchmark-vessel: ## Run only the Vessel side of the startup benchmark
	cd benchmarks/vessel-startup && ../../gradlew run -PbeanCount=$(BEANS)

benchmark-spring: ## Run only the Spring side of the startup benchmark
	cd benchmarks/spring-comparison && ../../gradlew run -PbeanCount=$(BEANS)

publish-local: ## Publish every library module to the local Maven repository (~/.m2) — safe, nothing leaves your machine
	$(GRADLE) publishToMavenLocal

sign-check: ## Sign every library module's artifacts with the configured GPG key, without publishing anywhere — verifies signing works before a real release
	$(GRADLE) signMavenPublication

publish: ## Publish every library module to Maven Central — irreversible, requires gradle.properties credentials; still needs a manual "Publish" click on the Central Portal afterward
	$(GRADLE) publishToMavenCentral

format-check: ## Fail if any Portuguese text has leaked into source code (code/tests must stay English — CLAUDE.md is the only exception, and it lives outside src/)
	@files=$$(grep -rlE '[áàâãéêíóôõúçÁÀÂÃÉÊÍÓÔÕÚÇ]' \
		vessel-core/src vessel-http/src vessel-web/src vessel-config/src vessel-app/src vessel-examples/src \
		2>/dev/null); \
	if [ -n "$$files" ]; then \
		echo "Portuguese text found in source:"; echo "$$files"; exit 1; \
	else \
		echo "clean — no stray Portuguese in source"; \
	fi
