package com.aresus.cliper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.aresus.cliper", "com.aresus.cliper.Api"})
public class CliperApplication {

	public static void main(String[] args) {
		SpringApplication.run(CliperApplication.class, args);
	}

}