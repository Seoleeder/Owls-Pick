package io.github.seoleeder.owls_pick;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan("io.github.seoleeder.owls_pick")
@SpringBootApplication
public class OwlsPickApplication {

	public static void main(String[] args) {
		SpringApplication.run(OwlsPickApplication.class, args);
	}

}
