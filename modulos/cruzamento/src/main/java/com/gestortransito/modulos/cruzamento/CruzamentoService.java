package com.gestortransito.modulos.cruzamento;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;
import com.gestortransito.modulos.contratos.mensagens.DadosGrafoSimples;
import com.gestortransito.modulos.contratos.mensagens.SistemaConfigSimplificado;
import com.gestortransito.modulos.cruzamento.enums.OrquestradorComandos;
import com.gestortransito.modulos.cruzamento.enums.StatusSinal;
import com.gestortransito.modulos.cruzamento.kafka.CruzamentoProducer;
import com.gestortransito.modulos.cruzamento.model.Cruzamento;
import com.gestortransito.modulos.cruzamento.repository.CruzamentoRepository;

@Service
public class CruzamentoService {

    private final CruzamentoRepository repository;
    private final CruzamentoProducer producer;

    public CruzamentoService(CruzamentoProducer producer, CruzamentoRepository repository) {
        this.producer = producer;
        this.repository = repository;
        System.out.println("=== CruzamentoService inicializado - Monitoramento programado a cada 5 segundos ===");
    }

    public void aplicarConfiguracao(SistemaConfigSimplificado config) {
        if (config == null || config.getDadosGrafo() == null) {
            System.out.println("[CONFIG] Configuração ignorada: dados de grafo ausentes.");
            return;
        }

        DadosGrafoSimples grafo = config.getDadosGrafo();
        List<String> nodos = grafo.getNodos();
        List<String> arestas = grafo.getArestas();
        if (nodos == null || arestas == null) {
            System.out.println("[CONFIG] Configuração incompleta: nodos ou arestas ausentes.");
            return;
        }

        Map<String, Map<String, Integer>> filasPorCruzamento = construirFilasPorCruzamento(nodos, arestas);
        filasPorCruzamento.forEach((id, filas) -> {
            Cruzamento cruzamento = repository.findById(id).orElseGet(() -> new Cruzamento(id, filas));
            cruzamento.atualizarFilas(filas);
            cruzamento.setStatusSinalHorizontal(StatusSinal.VERDE);
            cruzamento.setStatusSinalVertical(StatusSinal.VERMELHO);
            cruzamento.setInicioEsperaHorizontal(0);
            cruzamento.setInicioEsperaVertical(0);
            repository.save(cruzamento);
        });

        System.out.println("[CONFIG] Configuração aplicada: " + nodos.size() + " cruzamentos inicializados.");
    }

    public void adicionarVeiculoNaFila(String idVia, String idCruzamento) {
        Cruzamento cruzamento = repository.findById(idCruzamento)
                .orElseGet(() -> inicializarCruzamentoDinamico(idCruzamento, idVia));

        int filaAtual = cruzamento.getFilasPorVia().getOrDefault(idVia, 0);
        cruzamento.getFilasPorVia().put(idVia, filaAtual + 1);

        boolean isHorizontal = cruzamento.isViaHorizontal(idVia);
        StatusSinal statusSinal = isHorizontal ? cruzamento.getStatusSinalHorizontal() : cruzamento.getStatusSinalVertical();
        long inicioEspera = isHorizontal ? cruzamento.getInicioEsperaHorizontal() : cruzamento.getInicioEsperaVertical();

        if (filaAtual == 0 && statusSinal == StatusSinal.VERMELHO && inicioEspera == 0) {
            long novoTimestamp = System.currentTimeMillis();
            if (isHorizontal) {
                cruzamento.setInicioEsperaHorizontal(novoTimestamp);
            } else {
                cruzamento.setInicioEsperaVertical(novoTimestamp);
            }
        }

        repository.save(cruzamento);
    }

    public void executarComando(String idCruzamentoAlvo, String comando) {
        Cruzamento cruzamento = repository.findById(idCruzamentoAlvo)
                .orElseThrow(() -> new RuntimeException("Cruzamento alvo não encontrado: " + idCruzamentoAlvo));

        if (comando.toUpperCase().equals(OrquestradorComandos.ABRIR.name())) {
            StatusSinal novoStatusHorizontal = (cruzamento.getStatusSinalHorizontal() == StatusSinal.VERDE) 
                                                ? StatusSinal.VERMELHO : StatusSinal.VERDE;
            StatusSinal novoStatusVertical = (cruzamento.getStatusSinalVertical() == StatusSinal.VERDE) 
                                              ? StatusSinal.VERMELHO : StatusSinal.VERDE;

            cruzamento.setStatusSinalHorizontal(novoStatusHorizontal);
            cruzamento.setStatusSinalVertical(novoStatusVertical);

            if (novoStatusHorizontal == StatusSinal.VERDE) {
                cruzamento.getFilasPorVia().keySet().stream()
                    .filter(cruzamento::isViaHorizontal)
                    .forEach(key -> cruzamento.getFilasPorVia().put(key, 0));
                cruzamento.setInicioEsperaHorizontal(0);
            }
            
            if (novoStatusVertical == StatusSinal.VERDE) {
                cruzamento.getFilasPorVia().keySet().stream()
                    .filter(key -> !cruzamento.isViaHorizontal(key))
                    .forEach(key -> cruzamento.getFilasPorVia().put(key, 0));
                cruzamento.setInicioEsperaVertical(0);
            }

        } else {
            System.err.println("Comando desconhecido ou FECHAR recebido: " + comando);
            return;
        }

        repository.save(cruzamento);
        System.out.println("[COMANDO] INVERSÃO para cruzamento " + idCruzamentoAlvo);
    }

    @Scheduled(fixedRate = 5000)
    public void iniciarMonitoramento() {
        final long LIMITE_ESPERA_MS = 10 * 1000;

        System.out.println("[MONITORAMENTO] Verificando " + repository.count() + " cruzamentos...");
        repository.findAll().forEach(cruzamento -> {
            checarEAlertar(cruzamento, true, LIMITE_ESPERA_MS);
            checarEAlertar(cruzamento, false, LIMITE_ESPERA_MS);
        });
    }
    
    private void checarEAlertar(Cruzamento cruzamento, boolean isHorizontal, final long LIMITE_ESPERA_MS) {
        StatusSinal statusSinal = isHorizontal ? cruzamento.getStatusSinalHorizontal() : cruzamento.getStatusSinalVertical();
        long inicioEspera = isHorizontal ? cruzamento.getInicioEsperaHorizontal() : cruzamento.getInicioEsperaVertical();
        
        boolean temCarroNaFila = cruzamento.getFilasPorVia().entrySet().stream()
                                    .filter(entry -> cruzamento.isViaHorizontal(entry.getKey()) == isHorizontal)
                                    .anyMatch(entry -> entry.getValue() > 0);

        if (temCarroNaFila && statusSinal == StatusSinal.VERMELHO && inicioEspera > 0) {
            long tempoDecorrido = System.currentTimeMillis() - inicioEspera;

            if (tempoDecorrido >= LIMITE_ESPERA_MS) {
                String sentido = isHorizontal ? "HORIZONTAL" : "VERTICAL";
                System.out.println("[ALERTA] Cruzamento " + cruzamento.getId() + " - Sentido " + sentido + ": espera > 10s!");

                CruzamentoAlerta alerta = new CruzamentoAlerta();
                alerta.setIdCruzamento(cruzamento.getId());
                alerta.setMensagem("Tempo de espera excedido no sentido " + sentido);
                alerta.setTempoEsperaSegundos((int) (tempoDecorrido / 1000));

                producer.enviarAlerta(alerta);

                long novoTimestamp = System.currentTimeMillis();
                if (isHorizontal) {
                    cruzamento.setInicioEsperaHorizontal(novoTimestamp);
                } else {
                    cruzamento.setInicioEsperaVertical(novoTimestamp);
                }
                repository.save(cruzamento);
            }
        } else if (statusSinal == StatusSinal.VERDE || !temCarroNaFila) { 
            if (inicioEspera != 0) {
                if (isHorizontal) {
                    cruzamento.setInicioEsperaHorizontal(0);
                } else {
                    cruzamento.setInicioEsperaVertical(0);
                }
                repository.save(cruzamento);
            }
        }
    }

    private Cruzamento inicializarCruzamentoDinamico(String idCruzamento, String idVia) {
        Map<String, Integer> filas = new HashMap<>();
        filas.put(idVia, 0);
        Cruzamento novo = new Cruzamento(idCruzamento, filas);
        return repository.save(novo);
    }

    private Map<String, Map<String, Integer>> construirFilasPorCruzamento(List<String> nodos, List<String> arestas) {
        Map<String, Map<String, Integer>> filas = new HashMap<>();
        for (String nodo : nodos) {
            filas.put(nodo, new HashMap<>());
        }

        for (String aresta : arestas) {
            String[] partes = aresta.split("-");
            if (partes.length < 2) {
                continue;
            }
            String origem = partes[0];
            String destino = partes[1];
            Map<String, Integer> filasDestino = filas.getOrDefault(destino, new HashMap<>());
            filasDestino.put(aresta, 0);
            filas.put(destino, filasDestino);
        }
        return filas;
    }
}
