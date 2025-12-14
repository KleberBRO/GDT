package com.gestortransito.modulos.cruzamento.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;

@Service
public class CruzamentoProducer {

    // Injeta o template do Kafka usar os recursos da ferramente e enviar mensagens
    //vai fornecer o método send() para publicar mensagens
    //key= garante que todas as mensagens de um mesmo recurso caiam na mesma partição do Kafka
    //Object = mensagem em si
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CruzamentoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    } //construtor


    //envia alerta(CruzamentoAlerta) de uma espera longa no tópico cruzamento.alerta para o orquestrador consumir
    public void enviarAlerta(CruzamentoAlerta alerta) {
        String idCruzamento= alerta.getIdCruzamento();// A chave (key) da mensagem deve ser o ID do cruzamento (importante para particionamento)
        kafkaTemplate.send("cruzamento.alerta", idCruzamento, alerta) 
                .whenComplete((result, ex) -> {
                    if (ex == null) { //ex é uma exceção que pode ocorrer no send
                        System.out.println("Alerta enviado! Cruzamento: " + idCruzamento);
                    } else {
                        System.err.println("Erro ao enviar Alerta: " + ex.getMessage());
                    }
                });
        System.out.println("Alerta de espera longa para: " + idCruzamento);
    }
}