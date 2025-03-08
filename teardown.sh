#!/bin/bash

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

# Execute function
kill_existing

kubectl delete pods --all --grace-period=0 --force &

sleep 3

kubectl delete deployment --all

echo "All existing port-forwarding processes have been stopped."

eval $(minikube docker-env)

docker ps -a --filter "ancestor=user-service" -q | xargs docker rm -f
docker ps -a --filter "ancestor=wallet-service" -q | xargs docker rm -f
docker ps -a --filter "ancestor=marketplace-service" -q | xargs docker rm -f
docker ps -a --filter "ancestor=h2db" -q | xargs docker rm -f


docker images user-service -q | xargs docker rmi
docker images wallet-service -q | xargs docker rmi
docker images marketplace-service -q | xargs docker rmi
docker images h2db -q | xargs docker rmi

echo "All existing images have been removed."

minikube stop

echo "Minikube has been stopped."

echo "PLEASE WAIT ATLEAST 1 MINUTE BEFORE RUNNING START.SH"