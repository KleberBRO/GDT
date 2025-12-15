package com.gestortransito.modulos.orquestrador;

import org.springframework.stereotype.Service;

import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;
import com.gestortransito.modulos.contratos.mensagens.OrquestradorComando;
import com.gestortransito.modulos.contratos.mensagens.OrquestradorComandos;
import com.gestortransito.modulos.contratos.mensagens.SistemaConfigSimplificado;

@Service
public class OrquestradorService{

    private final OrquestradorProducer producer;
    private SistemaConfigSimplificado configAtual;

    public OrquestradorService(OrquestradorProducer producer) {
        this.producer = producer;
    }

   //com o alerta, ele manda o comando para o cruzamento fechar
    public void tratarAlerta(CruzamentoAlerta alerta){
    String idCruzamento = alerta.getIdCruzamento();
    String sentidoComProblema = alerta.getSentidoComProblema();
    int tempoEspera = alerta.getTempoEsperaSegundos();
    
    // Alerta recebido = Comando de ABRIR/INVERTER enviado (Remoção da checagem de Prioridade)
    System.out.println("ALERTA RECEBIDO DO CRUZAMENTO " + idCruzamento);
    System.out.println("  → Sentido com problema: " + sentidoComProblema);
    System.out.println("  → Tempo de espera: " + tempoEspera + "s");
    System.out.println("  → Enviando comando ABRIR para inversão de semáforos...");
    
    OrquestradorComando comando = new OrquestradorComando();
    comando.setIdCruzamentoAlvo(idCruzamento);
    comando.setComando(OrquestradorComandos.ABRIR.name()); 
    comando.setIdTransacao("CMD_" + System.currentTimeMillis()); 

    producer.enviarComando(comando);
}

public void configurarSistema(SistemaConfigSimplificado config) {
        this.configAtual = config;
        
        System.out.println("--- CONFIGURAÇÃO DO SISTEMA RECEBIDA ---");
        System.out.println("Evento: " + config.getTipoEvento());
        System.out.println("Quantidade de veículos: " + config.getQtdVeiculos());
        
        // Verifica se o grafo (DadosGrafo) foi enviado:
        if (config.getDadosGrafo() != null) {
            System.out.println("Grafo recebido. Total de nodos: " + config.getDadosGrafo().getNodos().size());
            System.out.println("Total de arestas: " + config.getDadosGrafo().getArestas().size());
        } else {
             System.out.println("ATENÇÃO: DadosGrafo não foi fornecido na configuração.");
        }
    }

}