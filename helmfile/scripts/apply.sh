#!/bin/bash
if [[ $BRANCH == *"/"* ]]; then
  export BRANCH=$(echo $BRANCH | awk -F'/' '{print $3}')
fi
echo "Deploying to: $BRANCH" 
az account set -s $SUBSCRIPTION
az aks get-credentials --admin --resource-group $RG_DEV --name $K8S_CLUSTER_NAME
cd ..
helmfile -e $TARGET apply