package com.gestortransito.modulos.orquestrador;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;
import com.gestortransito.modulos.contratos.mensagens.SistemaConfigSimplificado;
import java.util.Map;

@Component
public class OrquestradorConsumer {
    private final OrquestradorService orquestradorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrquestradorConsumer(OrquestradorService orquestradorService) {
        this.orquestradorService = orquestradorService;
    } //construtor

  
    //receber alertas para um comando imediato no tópico cruzamento.alerta
    @KafkaListener(topics = "cruzamento.alerta", groupId = "cruzamento-group")
    public void handleCruzamentoAlerta(Map<String, Object> message) {
        try {
            CruzamentoAlerta alerta = objectMapper.convertValue(message, CruzamentoAlerta.class);
            orquestradorService.tratarAlerta(alerta); // Lógica de tratamento
        } catch (Exception e) {
            System.err.println("Erro ao desserializar CruzamentoAlerta: " + e.getMessage());
        }
    }

    // NOVO: Recebe a configuração do grafo do front/módulo Visualizar
    @KafkaListener(topics = "sistema.configuracao", groupId = "orquestrador-config-group")
    public void handleSistemaConfig(Map<String, Object> message) {
        try {
            System.out.println("Configuração do grafo ('sistema.configuracao') recebida.");
            SistemaConfigSimplificado config = objectMapper.convertValue(message, SistemaConfigSimplificado.class);
            orquestradorService.configurarSistema(config);
        } catch (Exception e) {
            System.err.println("Erro ao desserializar SistemaConfigSimplificado: " + e.getMessage());
        }
    }
}