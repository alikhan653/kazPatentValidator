package kz.it.patentparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PatentParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatentParserApplication.class, args);
    }

}
