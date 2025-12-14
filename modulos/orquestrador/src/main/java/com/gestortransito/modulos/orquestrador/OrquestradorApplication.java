package com.gestortransito.modulos.orquestrador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class OrquestradorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrquestradorApplication.class, args);
    }

}
