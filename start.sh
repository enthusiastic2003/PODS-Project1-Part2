#!/bin/bash

minikube start

eval $(minikube docker-env)

docker build --no-cache -t marketplace-service ./products
docker build --no-cache -t wallet-service ./wallets
docker build --no-cache -t user-service ./user 
docker build --no-cache -t h2db ./h2_database

# Apply Kubernetes configurations
kubectl apply -f ./wallets/wallet-deployment.yaml \
              -f ./user/user-deployment.yaml \
              -f ./products/products-deployment.yaml \
              -f ./h2_database/h2db-deployment.yaml \
              -f ./h2_database/h2db-service.yaml

# Restart all deployments
# kubectl rollout restart deployment

echo "WAIT FOR 20 SECONDS BEFORE STARTING PORT FORWARDING"
sleep 20

# Define ports and their corresponding services
declare -A PORT_SERVICE_MAP
PORT_SERVICE_MAP["8080"]="service/user-service"
PORT_SERVICE_MAP["8081"]="service/marketplace-service"
PORT_SERVICE_MAP["8082"]="service/wallet-service"
PORT_SERVICE_MAP["9081"]="service/h2db-service"
PORT_SERVICE_MAP["9082"]="service/h2db-service"

# Function to restart port-forwarding with correct mapping
restart_port_forward() {
    for port in "${!PORT_SERVICE_MAP[@]}"; do
        service="${PORT_SERVICE_MAP[$port]}"
        echo "Starting port-forward on $service at port $port..."
        kubectl port-forward "$service" $port:$port &  # Run in background
        sleep 1  # Give some time for each port-forward to start
    done
}

# Execute function
restart_port_forward

echo "Port forwarding started for:"
for port in "${!PORT_SERVICE_MAP[@]}"; do
    echo "  - ${PORT_SERVICE_MAP[$port]} -> $port"
done
echo "PLEASE ENTER YOUR PASSWORD TO START MINIKUBE TUNNEL. KEEP THIS TERMINAL OPEN. RUN TESTS IN A NEW TERMINAL. PRESS CTRL+C TO STOP MINIKUBE TUNNEL. RUN TEARDOWN.SH TO STOP PORT FORWARDING."
echo "If it doesnt work, please try first stopping this terminal with ctrl+c. Then running 'minikube tunnel' , then try tests in another terminal."
minikube tunnel
