eval $(minikube docker-env)

docker build --no-cache -t marketplace-service ./products
docker build --no-cache -t wallet-service ./wallets
docker build --no-cache -t user-service ./user 

