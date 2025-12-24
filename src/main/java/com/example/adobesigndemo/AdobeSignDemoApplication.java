package com.example.adobesigndemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.adobesigndemo.config.AdobeSignProperties;

@SpringBootApplication
@EnableConfigurationProperties(AdobeSignProperties.class)
public class AdobeSignDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdobeSignDemoApplication.class, args);
	}

}
