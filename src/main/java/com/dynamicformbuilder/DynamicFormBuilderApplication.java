package com.dynamicformbuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DynamicFormBuilderApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamicFormBuilderApplication.class, args);
	}

}
