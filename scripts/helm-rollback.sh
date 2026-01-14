#!/bin/bash
set -e

ENVIRONMENT=${1:-"dev"}
REVISION=${2:-"1"}
NAMESPACE=${3:-"fintech"}
RELEASE_NAME="analytics-$ENVIRONMENT"

echo "Rolling back $RELEASE_NAME to revision $REVISION..."

helm rollback $RELEASE_NAME $REVISION -n $NAMESPACE --wait

echo "Rollback completed!"