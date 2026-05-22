package com.streamforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class StreamforgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(StreamforgeApplication.class, args);
	}

}
