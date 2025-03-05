#!/bin/bash

kubectl rollout restart deployment

# Define ports and their corresponding services
declare -A PORT_SERVICE_MAP
PORT_SERVICE_MAP["8080"]="service/user-service"
PORT_SERVICE_MAP["8081"]="service/marketplace-service"
PORT_SERVICE_MAP["8082"]="service/wallet-service"
PORT_SERVICE_MAP["9081"]="service/h2db-service"
PORT_SERVICE_MAP["9082"]="service/h2db-service"

# Function to kill existing port-forward processes
kill_existing() {
    for port in "${!PORT_SERVICE_MAP[@]}"; do
        pid=$(lsof -ti :$port)  # Get process ID using the port
        if [ ! -z "$pid" ]; then
            echo "Stopping port-forward on port $port (PID: $pid)"
            kill -9 "$pid"
        fi
    done
}

# Function to restart port-forwarding with correct mapping
restart_port_forward() {
    for port in "${!PORT_SERVICE_MAP[@]}"; do
        service="${PORT_SERVICE_MAP[$port]}"
        echo "Starting port-forward on $service at port $port..."
        kubectl port-forward "$service" $port:$port &  # Run in background
        sleep 1  # Give some time for each port-forward to start
    done
}

# Execute functions
kill_existing
restart_port_forward

echo "Port forwarding restarted for:"
for port in "${!PORT_SERVICE_MAP[@]}"; do
    echo "  - ${PORT_SERVICE_MAP[$port]} -> $port"
done
