#!/bin/bash

# Скрипт для сборки и пуша Docker образа
set -e

# Переменные
IMAGE_NAME="fintech/analytics-service"
VERSION=${1:-"latest"}
REGISTRY=${2:-"your-registry.com"}

echo "Building Docker image..."
docker build -t ${IMAGE_NAME}:${VERSION} .

echo "Tagging image for registry..."
docker tag ${IMAGE_NAME}:${VERSION} ${REGISTRY}/${IMAGE_NAME}:${VERSION}

echo "Pushing image to registry..."
docker push ${REGISTRY}/${IMAGE_NAME}:${VERSION}

echo "Image pushed successfully: ${REGISTRY}/${IMAGE_NAME}:${VERSION}"