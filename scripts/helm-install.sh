#!/bin/bash
set -e

ENVIRONMENT=${1:-"dev"}
NAMESPACE=${2:-"fintech"}
RELEASE_NAME="analytics-$ENVIRONMENT"

echo "Deploying to $ENVIRONMENT environment..."

# Установка зависимостей
echo "Updating Helm dependencies..."
helm dependency update ./helm

case $ENVIRONMENT in
  dev)
    VALUES_FILE="values-dev.yaml"
    ;;
  staging)
    VALUES_FILE="values-staging.yaml"
    ;;
  prod)
    VALUES_FILE="values-prod.yaml"
    ;;
  *)
    VALUES_FILE="values.yaml"
    ;;
esac

echo "Using values file: $VALUES_FILE"

# Установка/обновление релиза
if helm list -n $NAMESPACE | grep -q $RELEASE_NAME; then
  echo "Upgrading existing release..."
  helm upgrade $RELEASE_NAME ./helm \
    -n $NAMESPACE \
    -f ./helm/$VALUES_FILE \
    --set app.image.tag=$IMAGE_TAG \
    --atomic \
    --timeout 10m \
    --cleanup-on-fail
else
  echo "Installing new release..."
  helm install $RELEASE_NAME ./helm \
    -n $NAMESPACE \
    -f ./helm/$VALUES_FILE \
    --create-namespace \
    --set app.image.tag=$IMAGE_TAG \
    --atomic \
    --timeout 10m \
    --wait
fi

echo "Deployment completed!"
echo "Release: $RELEASE_NAME"
echo "Namespace: $NAMESPACE"