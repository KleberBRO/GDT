package com.gestortransito.modulos.cruzamento.kafka;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import com.gestortransito.modulos.contratos.mensagens.OrquestradorComando;
import com.gestortransito.modulos.contratos.mensagens.SensorVeiculo;
import com.gestortransito.modulos.cruzamento.CruzamentoService;

@Component
public class CruzamentoConsumer {

    // Injeta a Lógica principal para processar os comandos/eventos
    private final CruzamentoService cruzamentoService;

    // 1. Variável para armazenar o ID deste cruzamento específico
    // @Value("${cruzamento.id}") injeta o valor de uma propriedade (ex: cruzamento.id=cruzamento_01)
    // lida ao iniciar o container/aplicação.
    @Value("${cruzamento.id}")
    private String meuIdCruzamento;

    public CruzamentoConsumer(CruzamentoService cruzamentoService) {
        this.cruzamentoService = cruzamentoService;
    } //construtor

    //recebe/consome as mensagens do sensor veículo (SensorVeiculo) no tópico sensor.veiculo
    //isso toda vez que um veículo chega na via
    @KafkaListener(topics = "sensor.veiculo", groupId = "cruzamento-group") 
    public void handleSensorVeiculo(SensorVeiculo veiculo) {
        System.out.println("Veículo detectado em: " + veiculo.getIdVia() + " para o cruzamento: " + meuIdCruzamento);
        cruzamentoService.adicionarVeiculoNaFila(veiculo.getIdVia(), meuIdCruzamento); // chama a lógica de serviço para atualizar a fila de veículos na via
    }

    //recebe/consome o comando OrquestradorComando do orquestrador no tópico orquestrador.comando
    //isso para abrir ou fechar o semáforo
    //@KafkaListener: Diz ao Spring para criar um Consumidor Kafka e vinculá-lo a esta função
    @KafkaListener(topics = "orquestrador.comando", groupId = "cruzamento-group") 
    public void handleOrquestradorComando(OrquestradorComando comando) {

        cruzamentoService.executarComando(comando.getIdCruzamentoAlvo(), comando.getComando());
        // comando.getIdCruzamentoAlvo()= ID para saber qual semáforo mudar
        // comando.getComando() = O comando ("ABRIR" ou "FECHAR")
        System.out.println("Recebido comando: " + comando.getComando() + " para o cruzamento: " + comando.getIdCruzamentoAlvo());
    }
}