package com.mmtext.editorservermain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableKafka
@ConfigurationPropertiesScan("com.mmtext.editorservermain.config")
public class EditorServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EditorServerApplication.class, args);
	}

}
