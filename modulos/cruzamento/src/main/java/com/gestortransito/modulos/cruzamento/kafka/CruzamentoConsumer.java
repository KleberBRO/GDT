package com.gestortransito.modulos.cruzamento.kafka;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestortransito.modulos.contratos.mensagens.OrquestradorComando;
import com.gestortransito.modulos.contratos.mensagens.SistemaConfigSimplificado;
import com.gestortransito.modulos.cruzamento.CruzamentoService;
import java.util.Map;

@Component
public class CruzamentoConsumer {

    private final CruzamentoService cruzamentoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cruzamento.id}")
    private String meuIdCruzamento;

    public CruzamentoConsumer(CruzamentoService cruzamentoService) {
        this.cruzamentoService = cruzamentoService;
    }

    @KafkaListener(topics = "sistema.configuracao", groupId = "cruzamento-group")
    public void handleSistemaConfig(Map<String, Object> message) {
        try {
            System.out.println("=== Configuração do sistema recebida no cruzamento ===");
            SistemaConfigSimplificado config = objectMapper.convertValue(message, SistemaConfigSimplificado.class);
            cruzamentoService.aplicarConfiguracao(config);
        } catch (Exception e) {
            System.err.println("Erro ao processar configuração: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //recebe/consome as mensagens do sensor veículo (SensorVeiculo) no tópico sensor.veiculo
    //isso toda vez que um veículo chega na via
    @KafkaListener(topics = "sensor.veiculo", groupId = "cruzamento-group") 
    public void handleSensorVeiculo(java.util.Map<String, Object> message) {
        try {
            String idVia = (String) message.get("idVia");
            System.out.println("✓ Veículo detectado em: " + idVia + " para o cruzamento: " + meuIdCruzamento);
            cruzamentoService.adicionarVeiculoNaFila(idVia, meuIdCruzamento); // chama a lógica de serviço para atualizar a fila de veículos na via
        } catch (Exception e) {
            System.err.println("Erro ao processar veículo: " + e.getMessage());
        }
    }

    //recebe/consome o comando OrquestradorComando do orquestrador no tópico orquestrador.comando
    //isso para abrir ou fechar o semáforo
    //@KafkaListener: Diz ao Spring para criar um Consumidor Kafka e vinculá-lo a esta função
    @KafkaListener(topics = "orquestrador.comando", groupId = "cruzamento-group") 
    public void handleOrquestradorComando(java.util.Map<String, Object> message) {
        try {
            System.out.println("[CONSUMER] Recebendo comando do Kafka");
            String idCruzamento = (String) message.get("idCruzamentoAlvo");
            String comando = (String) message.get("comando");
            System.out.println("[CONSUMER] idCruz=" + idCruzamento + ", cmd=" + comando);
            cruzamentoService.executarComando(idCruzamento, comando);
            System.out.println("[CONSUMER] Comando processado com sucesso!");
        } catch (Exception e) {
            System.err.println("[CONSUMER ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }
}