package ott.j4jg_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class J4JGBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(J4JGBeApplication.class, args);
    }

}
