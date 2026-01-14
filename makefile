.PHONY: help build test helm-lint helm-install helm-upgrade helm-uninstall

# Colors
RED=\033[0;31m
GREEN=\033[0;32m
YELLOW=\033[1;33m
NC=\033[0m # No Color

help:
	@echo "FinTech Analytics Service Management"
	@echo ""
	@echo "Commands:"
	@echo "  ${GREEN}build${NC}         - Build the project"
	@echo "  ${GREEN}test${NC}          - Run tests"
	@echo "  ${GREEN}run${NC}           - Run locally"
	@echo "  ${GREEN}docker-build${NC}  - Build Docker image"
	@echo "  ${GREEN}docker-run${NC}    - Run with Docker Compose"
	@echo ""
	@echo "Helm Commands:"
	@echo "  ${GREEN}helm-deps${NC}     - Update Helm dependencies"
	@echo "  ${GREEN}helm-lint${NC}     - Lint Helm chart"
	@echo "  ${GREEN}helm-install${NC}  - Install Helm chart (dev/staging/prod)"
	@echo "  ${GREEN}helm-upgrade${NC}  - Upgrade Helm chart"
	@echo "  ${GREEN}helm-uninstall${NC}- Uninstall Helm chart"
	@echo "  ${GREEN}helm-status${NC}   - Check Helm release status"
	@echo "  ${GREEN}helm-rollback${NC} - Rollback to previous version"
	@echo ""
	@echo "Examples:"
	@echo "  make helm-install env=dev"
	@echo "  make helm-upgrade env=prod tag=v1.2.3"

# Build commands
build:
	@echo "${YELLOW}Building project...${NC}"
	./gradlew clean build

test:
	@echo "${YELLOW}Running tests...${NC}"
	./gradlew test

run:
	@echo "${YELLOW}Starting application locally...${NC}"
	SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

docker-build:
	@echo "${YELLOW}Building Docker image...${NC}"
	docker build -t fintech/analytics-service:latest .

docker-run:
	@echo "${YELLOW}Starting with Docker Compose...${NC}"
	docker-compose up -d

# Helm commands
helm-deps:
	@echo "${YELLOW}Updating Helm dependencies...${NC}"
	helm dependency update ./helm

helm-lint:
	@echo "${YELLOW}Linting Helm chart...${NC}"
	helm lint ./helm
	helm template ./helm --dry-run

helm-install:
	@echo "${YELLOW}Installing Helm chart for $(or ${env},dev) environment...${NC}"
	./scripts/helm-install.sh ${env} ${namespace}

helm-upgrade:
	@echo "${YELLOW}Upgrading Helm chart for $(or ${env},dev) environment...${NC}"
	helm upgrade analytics-${env} ./helm \
		-n ${namespace:-fintech} \
		-f ./helm/values-${env}.yaml \
		--set app.image.tag=${tag:-latest} \
		--atomic \
		--timeout 10m

helm-uninstall:
	@echo "${YELLOW}Uninstalling Helm chart for $(or ${env},dev) environment...${NC}"
	./scripts/helm-uninstall.sh ${env} ${namespace}

helm-status:
	@echo "${YELLOW}Checking Helm release status...${NC}"
	helm list -n ${namespace:-fintech}
	helm status analytics-${env} -n ${namespace:-fintech}

helm-rollback:
	@echo "${YELLOW}Rolling back to revision ${revision}...${NC}"
	./scripts/helm-rollback.sh ${env} ${revision} ${namespace}

# Kubernetes commands
k8s-status:
	@echo "${YELLOW}Kubernetes status...${NC}"
	kubectl get pods,svc,ingress,hpa -n ${namespace:-fintech} -l app.kubernetes.io/name=fintech-analytics

k8s-logs:
	@echo "${YELLOW}Showing logs...${NC}"
	kubectl logs -n ${namespace:-fintech} -l app.kubernetes.io/name=fintech-analytics --tail=100 -f

k8s-exec:
	@echo "${YELLOW}Executing into pod...${NC}"
	kubectl exec -n ${namespace:-fintech} -it $(kubectl get pods -n ${namespace:-fintech} -l app.kubernetes.io/name=fintech-analytics -o jsonpath='{.items[0].metadata.name}') -- sh

# Database commands
db-portforward:
	@echo "${YELLOW}Port-forwarding database...${NC}"
	kubectl port-forward -n ${namespace:-fintech} svc/analytics-${env}-postgresql 5432:5432

# Cleanup
clean:
	@echo "${YELLOW}Cleaning...${NC}"
	./gradlew clean
	rm -rf build
	docker system prune -f

# Monitoring
monitor:
	@echo "${YELLOW}Opening monitoring tools...${NC}"
	@echo "Grafana: http://localhost:3000"
	@echo "Prometheus: http://localhost:9090"
	@echo "Application: http://localhost:8080"