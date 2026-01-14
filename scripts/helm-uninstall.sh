#!/bin/bash
set -e

ENVIRONMENT=${1:-"dev"}
NAMESPACE=${2:-"fintech"}
RELEASE_NAME="analytics-$ENVIRONMENT"

echo "Uninstalling $RELEASE_NAME from $NAMESPACE..."

helm uninstall $RELEASE_NAME -n $NAMESPACE --wait

# Удаляем PVC если нужно
if [ "$ENVIRONMENT" = "dev" ]; then
  echo "Removing PVCs..."
  kubectl delete pvc -n $NAMESPACE -l app.kubernetes.io/instance=$RELEASE_NAME --ignore-not-found=true
fi

echo "Uninstall completed!"