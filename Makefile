.PHONY: help build clean test docker-build docker-up docker-down docker-logs docker-ps check-networks create-networks up-all down-all restart-all clean-all db-shell db-restore health db-up generate-jooq

# Default target
.DEFAULT_GOAL := help

# Variables
GRADLEW := ./gradlew
DOCKER_COMPOSE := docker compose
DOCKER_COMPOSE_FILES := -f docker-compose.yml -f docker-compose.monitoring.yml
COMPOSE_PROJECT_NAME := COMPOSE_PROJECT_NAME=k12
APP_NAME := k12-backend
POSTGRES_CONTAINER := k12-postgres

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

##@ General

help: ## Display this help message
	@echo "$(BLUE)K12 Backend - Makefile Commands$(NC)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"; printf "Usage: make $(GREEN)<target>$(NC)\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(YELLOW)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

build: ## Build the application (skip tests, skip jOOQ generation)
	@echo "$(BLUE)Building application...$(NC)"
	$(GRADLEW) assemble -x test -x generateJooq
	@echo "$(GREEN)Build complete!$(NC)"

build-full: ## Build the application with tests
	@echo "$(BLUE)Building application with tests...$(NC)"
	$(GRADLEW) build -x generateJooq
	@echo "$(GREEN)Build complete with tests!$(NC)"

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(GRADLEW) clean
	@echo "$(GREEN)Clean complete!$(NC)"

test: ## Run all tests
	@echo "$(BLUE)Running tests...$(NC)"
	$(GRADLEW) test
	@echo "$(GREEN)Tests complete!$(NC)"

test-integration: ## Run integration tests only
	@echo "$(BLUE)Running integration tests...$(NC)"
	$(GRADLEW) test --tests "*IntegrationTest"
	@echo "$(GREEN)Integration tests complete!$(NC)"

##@ Docker

docker-build: build ## Build Docker image (requires build first)
	@echo "$(BLUE)Building Docker image...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) build
	@echo "$(GREEN)Docker image built!$(NC)"

docker-build-no-cache: ## Build Docker image without cache
	@echo "$(BLUE)Building Docker image (no cache)...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) build --no-cache
	@echo "$(GREEN)Docker image built!$(NC)"

docker-up: ## Start application services only
	@echo "$(BLUE)Starting application services...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)Application services started!$(NC)"

docker-down: ## Stop application services
	@echo "$(BLUE)Stopping application services...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) down
	@echo "$(GREEN)Application services stopped!$(NC)"

docker-logs: ## Show application logs (follow mode)
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) logs -f

docker-logs-backend: ## Show backend logs only
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) logs -f k12-backend

docker-logs-postgres: ## Show postgres logs only
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) logs -f k12-postgres

docker-ps: ## Show running containers
	COMPOSE_PROJECT_NAME=k12 docker compose ps

docker-compose-validate: ## Validate docker-compose files
	@echo "$(BLUE)Validating docker-compose files...$(NC)"
	@docker compose -f docker-compose.yml config >/dev/null 2>&1 && echo "$(GREEN)✓ docker-compose.yml is valid$(NC)" || (echo "$(RED)✗ docker-compose.yml has errors$(NC)" && exit 1)
	@docker compose -f docker-compose.yml -f docker-compose.monitoring.yml config >/dev/null 2>&1 && echo "$(GREEN)✓ Combined docker-compose files are valid$(NC)" || (echo "$(RED)✗ Combined docker-compose files have errors$(NC)" && exit 1)
	@echo "$(GREEN)All docker-compose files are valid!$(NC)"

##@ Networks

check-networks: ## Check if required networks exist
	@echo "$(BLUE)Checking Docker networks...$(NC)"
	@docker network ls | grep -q "k12-monitoring" && echo "$(GREEN)✓ k12-monitoring exists$(NC)" || echo "$(YELLOW)✗ k12-monitoring missing$(NC)"

create-networks: ## Create required Docker networks
	@echo "$(BLUE)Creating Docker networks...$(NC)"
	@docker network ls | grep -q "k12-monitoring" || docker network create k12-monitoring
	@echo "$(GREEN)Networks created!$(NC)"

##@ Full Stack

up-all: docker-build ## Start full stack (app + monitoring)
	@echo "$(BLUE)Starting full stack...$(NC)"
	@echo "$(YELLOW)Removing conflicting containers if any...$(NC)"
	@docker ps -a --filter "name=k12" --format "{{.Names}}" | grep -E "^(k12-postgres|k12-backend|k12-redoc)$$" | xargs -r docker rm -fv 2>/dev/null || true
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) up -d --remove-orphans
	@echo "$(YELLOW)Waiting for services to be healthy...$(NC)"
	@sleep 10
	@echo "$(GREEN)Full stack started!$(NC)"
	@echo "$(YELLOW)Access the application at: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Access SigNoz observability at: http://localhost:3301$(NC)"

build-with-db: ## Build with database (for jOOQ code generation)
	@echo "$(BLUE)Starting database for build...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) up -d k12-postgres
	@echo "$(YELLOW)Waiting for database to be ready...$(NC)"
	@timeout 60 bash -c 'until docker exec $(POSTGRES_CONTAINER) pg_isready -U k12_user -d k12_db >/dev/null 2>&1; do sleep 1; done' || echo "$(RED)Database did not start in time$(NC)"
	@echo "$(BLUE)Building application...$(NC)"
	$(GRADLEW) assemble -x test
	@echo "$(GREEN)Build complete!$(NC)"
	@echo "$(BLUE)Stopping database...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) stop k12-postgres

down-all: ## Stop full stack
	@echo "$(BLUE)Stopping full stack...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) down
	@echo "$(GREEN)Full stack stopped!$(NC)"

restart-all: down-all up-all ## Restart full stack

clean-all: ## Remove all containers, volumes, and images for fresh start
	@echo "$(BLUE)Removing all containers, volumes, and images...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) down -v --remove-orphans
	@echo "$(YELLOW)Removing k12-monitoring network if unused...$(NC)"
	@docker network ls -q --filter "name=k12-monitoring" | xargs -r docker network rm 2>/dev/null || true
	@echo "$(GREEN)Cleanup complete!$(NC)"

##@ Database

db-up: ## Start database only (required for jOOQ code generation)
	@echo "$(BLUE)Starting PostgreSQL...$(NC)"
	COMPOSE_PROJECT_NAME=k12 $(DOCKER_COMPOSE) $(DOCKER_COMPOSE_FILES) up -d k12-postgres
	@echo "$(YELLOW)Waiting for database to be ready...$(NC)"
	@timeout 60 bash -c 'until docker exec $(POSTGRES_CONTAINER) pg_isready -U k12_user -d k12_db >/dev/null 2>&1; do sleep 1; done' || echo "$(RED)Database did not start in time$(NC)"
	@echo "$(GREEN)Database is ready!$(NC)"

generate-jooq: ## Run jOOQ code generation (requires database running)
	@echo "$(BLUE)Generating jOOQ code...$(NC)"
	$(GRADLEW) generateJooq
	@echo "$(GREEN)jOOQ code generated!$(NC)"

db-shell: ## Open PostgreSQL shell
	@echo "$(BLUE)Opening PostgreSQL shell...$(NC)"
	docker exec -it $(POSTGRES_CONTAINER) psql -U k12_user -d k12_db

db-restore: ## Restore database from file (usage: make db-restore FILE=backup.sql)
	@echo "$(BLUE)Restoring database from $(FILE)...$(NC)"
	docker exec -i $(POSTGRES_CONTAINER) psql -U k12_user -d k12_db < $(FILE)
	@echo "$(GREEN)Database restored!$(NC)"

db-backup: ## Backup database (usage: make db-backup FILE=backup.sql)
	@echo "$(BLUE)Backing up database to $(FILE)...$(NC)"
	docker exec $(POSTGRES_CONTAINER) pg_dump -U k12_user k12_db > $(FILE)
	@echo "$(GREEN)Database backed up to $(FILE)!$(NC)"

##@ Health & Monitoring

health: ## Check health of all services
	@echo "$(BLUE)Checking service health...$(NC)"
	@echo "\n$(YELLOW)PostgreSQL:$(NC)"
	@docker exec $(POSTGRES_CONTAINER) pg_isready -U k12_user -d k12_db 2>/dev/null && echo "  $(GREEN)✓ Healthy$(NC)" || echo "  $(RED)✗ Unhealthy$(NC)"
	@echo "\n$(YELLOW)Backend:$(NC)"
	@curl -sf http://localhost:8080/q/health >/dev/null 2>&1 && echo "  $(GREEN)✓ Healthy$(NC)" || echo "  $(RED)✗ Unhealthy$(NC)"
	@echo "\n$(YELLOW)Containers:$(NC)"
	@$(DOCKER_COMPOSE) ps

##@ CI/CD

ci: clean build-full test docker-build ## Full CI pipeline: clean, build, test, and docker build

deploy: up-all ## Deploy full stack to Docker

##@ Utilities

format: ## Format code with Spotless
	@echo "$(BLUE)Formatting code...$(NC)"
	$(GRADLEW) spotlessApply
	@echo "$(GREEN)Code formatted!$(NC)"

format-check: ## Check code formatting
	@echo "$(BLUE)Checking code formatting...$(NC)"
	$(GRADLEW) spotlessCheck

deps-update: ## Update Gradle dependencies
	@echo "$(BLUE)Updating dependencies...$(NC)"
	$(GRADLEW) dependencies
	@echo "$(GREEN)Dependencies updated!$(NC)"
