docker run -p 8080:8080 --rm --name user --add-host=host.docker.internal:host-gateway user-service &
docker run -p 8081:8080 --rm --name marketplace --add-host=host.docker.internal:host-gateway marketplace-service & 
docker run -p 8082:8080 --rm --name wallet --add-host=host.docker.internal:host-gateway wallet-service &



