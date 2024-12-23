package com.amazing.EverydayWiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EverydayWikiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EverydayWikiApplication.class, args);
	}

}
