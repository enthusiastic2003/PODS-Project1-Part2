mvn -f ./products/marketplace/pom.xml package &
mvn -f ./user/user-service/pom.xml package &
mvn -f ./wallets/wallet-service/pom.xml package &

wait  # Waits for all background jobs to finish
