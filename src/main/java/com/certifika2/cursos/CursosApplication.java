package com.certifika2.cursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CursosApplication {
    public static void main(String[] args) {
        SpringApplication.run(CursosApplication.class, args);
    }
}
