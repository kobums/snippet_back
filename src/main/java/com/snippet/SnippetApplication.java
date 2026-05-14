package com.snippet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SnippetApplication {

	public static void main(String[] args) {
		SpringApplication.run(SnippetApplication.class, args);
	}

}
