package com.gestortransito.modulos.cruzamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class CruzamentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CruzamentoApplication.class, args);
    }

}
