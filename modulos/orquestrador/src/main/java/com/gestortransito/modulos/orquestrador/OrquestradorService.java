package com.gestortransito.modulos.orquestrador;

import org.springframework.stereotype.Service;

import com.gestortransito.modulos.contratos.SistemaConfig;
import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;
import com.gestortransito.modulos.contratos.mensagens.OrquestradorComando;

@Service
public class OrquestradorService{

    private final OrquestradorProducer producer;


    public OrquestradorService(OrquestradorProducer producer) {
        this.producer = producer;
    }


   //com o alerta, ele manda o comando para o cruzamento fechar
    public void tratarAlerta(CruzamentoAlerta alerta){
    String idCruzamento = alerta.getIdCruzamento();
    
    // Alerta recebido = Comando de ABRIR/INVERTER enviado (Remoção da checagem de Prioridade)
    System.out.println("ALERTA RECEBIDO DO CRUZAMENTO " + idCruzamento + ". Enviando comando ABRIR para inversão de semáforos.");
    
    OrquestradorComando comando = new OrquestradorComando();
    comando.setIdCruzamentoAlvo(idCruzamento);
    comando.setComando(OrquestradorComandos.ABRIR.name()); 
    comando.setIdTransacao("CMD_" + System.currentTimeMillis()); 

    producer.enviarComando(comando);
}

public void configurarSistema(SistemaConfig config) {
        this.configAtual = config;
        
        System.out.println("--- CONFIGURAÇÃO DO SISTEMA RECEBIDA ---");
        System.out.println("Evento: " + config.getTipoEvento());
        
        // Verifica se o grafo (DadosGrafo) foi enviado:
        if (config.getDadosGrafo() != null) {
            int qtdCruzamentos = config.getDadosGrafo().getCruzamentos().size();
            System.out.println("Grafo recebido. Total de Cruzamentos: " + qtdCruzamentos);
            // Aqui você usaria config.getDadosGrafo().getArestas() para tomar decisões avançadas.
        } else {
             System.out.println("ATENÇÃO: DadosGrafo não foi fornecido na configuração.");
        }
    }

}