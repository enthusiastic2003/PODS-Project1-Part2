package com.sirjanhansda.pods.wallets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = "com.sirjanhansda.pods.wallets")
public class WalletsApplication {
	public static void main(String[] args) {
		SpringApplication.run(WalletsApplication.class, args);
	}

}
