package com.tvcanaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendTvCanariaApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendTvCanariaApplication.class, args);
	}

}
