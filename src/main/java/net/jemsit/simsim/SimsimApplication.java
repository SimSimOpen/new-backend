package net.jemsit.simsim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"net.jemsit"})
@EntityScan(basePackages = {"net.jemsit"})
@EnableJpaRepositories(basePackages = {"net.jemsit"})
public class SimsimApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimsimApplication.class, args);
	}

}
