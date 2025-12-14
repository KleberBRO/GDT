package com.gestortransito.modulos.cruzamento;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.gestortransito.modulos.contratos.mensagens.CruzamentoAlerta;
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
    }

    // 1. Quando um veículo chega no cruzamento
    public void adicionarVeiculoNaFila(String idVia, String idCruzamento) {
        // ... (Lógica igual, apenas atualizando a fila e o inicioEspera)
        Cruzamento cruzamento = repository.findById(idCruzamento)
                .orElseThrow(() -> new RuntimeException("Cruzamento não encontrado: " + idCruzamento));

        // Atualiza a fila da via específica (assumindo que idVia é uma via de CHEGADA)
        int filaAtual = cruzamento.getFilasPorVia().getOrDefault(idVia, 0);
        cruzamento.getFilasPorVia().put(idVia, filaAtual + 1);

        boolean isHorizontal = cruzamento.isViaHorizontal(idVia);
        StatusSinal statusSinal = isHorizontal ? cruzamento.getStatusSinalHorizontal() : cruzamento.getStatusSinalVertical();
        long inicioEspera = isHorizontal ? cruzamento.getInicioEsperaHorizontal() : cruzamento.getInicioEsperaVertical();

        // Lógica de início de espera: se for o primeiro carro E o sinal estiver VERMELHO.
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

    // 2. Executa o comando do orquestrador (sempre 'ABRIR')
    public void executarComando(String idCruzamentoAlvo, String comando) {

        Cruzamento cruzamento = repository.findById(idCruzamentoAlvo)
                .orElseThrow(() -> new RuntimeException("Cruzamento alvo não encontrado: " + idCruzamentoAlvo));

        if (comando.toUpperCase().equals(OrquestradorComandos.ABRIR.name())) {
            
            // Lógica de INVERSÃO de estado dos dois semáforos (Vertical <-> Horizontal)
            StatusSinal novoStatusHorizontal = (cruzamento.getStatusSinalHorizontal() == StatusSinal.VERDE) 
                                                ? StatusSinal.VERMELHO : StatusSinal.VERDE;
            StatusSinal novoStatusVertical = (cruzamento.getStatusSinalVertical() == StatusSinal.VERDE) 
                                              ? StatusSinal.VERMELHO : StatusSinal.VERDE;

            cruzamento.setStatusSinalHorizontal(novoStatusHorizontal);
            cruzamento.setStatusSinalVertical(novoStatusVertical);

            // Zera as filas e o contador de espera do sentido que acabou de abrir
            
            // Sentido HORIZONTAL acabou de abrir (ficou VERDE)
            if (novoStatusHorizontal == StatusSinal.VERDE) {
                // Zera as filas de todas as vias que são horizontais (vias de chegada)
                cruzamento.getFilasPorVia().keySet().stream()
                    .filter(cruzamento::isViaHorizontal)
                    .forEach(key -> cruzamento.getFilasPorVia().put(key, 0));
                cruzamento.setInicioEsperaHorizontal(0);
            }
            
            // Sentido VERTICAL acabou de abrir (ficou VERDE)
            if (novoStatusVertical == StatusSinal.VERDE) {
                // Zera as filas de todas as vias que são verticais (vias de chegada)
                cruzamento.getFilasPorVia().keySet().stream()
                    .filter(key -> !cruzamento.isViaHorizontal(key)) // Filtra as verticais
                    .forEach(key -> cruzamento.getFilasPorVia().put(key, 0));
                cruzamento.setInicioEsperaVertical(0);
            }

        } else {
            System.err.println("Comando desconhecido ou FECHAR recebido: " + comando);
            return;
        }

        repository.save(cruzamento);
        System.out.println("Comando executado: INVERSÃO para o cruzamento " + idCruzamentoAlvo + 
                           ". Novo status: H: " + cruzamento.getStatusSinalHorizontal() + 
                           " | V: " + cruzamento.getStatusSinalVertical());
    }

    // 3. Monitoramento de Espera (Limite alterado para 10 segundos)
    @Scheduled(fixedRate = 5000) // 5 segundos
    private void iniciarMonitoramento() {
        final long LIMITE_ESPERA_MS = 10 * 1000; // 10 segundos

        repository.findAll().forEach(cruzamento -> {
            checarEAlertar(cruzamento, true, LIMITE_ESPERA_MS); // Horizontal
            checarEAlertar(cruzamento, false, LIMITE_ESPERA_MS); // Vertical
        });
    }
    
    // Método auxiliar para checar e alertar para um sentido específico
    private void checarEAlertar(Cruzamento cruzamento, boolean isHorizontal, final long LIMITE_ESPERA_MS) {
        
        StatusSinal statusSinal = isHorizontal ? cruzamento.getStatusSinalHorizontal() : cruzamento.getStatusSinalVertical();
        long inicioEspera = isHorizontal ? cruzamento.getInicioEsperaHorizontal() : cruzamento.getInicioEsperaVertical();
        
        // Verifica se há carros na fila de alguma via neste sentido
        boolean temCarroNaFila = cruzamento.getFilasPorVia().entrySet().stream()
                                    .filter(entry -> cruzamento.isViaHorizontal(entry.getKey()) == isHorizontal)
                                    .anyMatch(entry -> entry.getValue() > 0);

        if (temCarroNaFila && statusSinal == StatusSinal.VERMELHO && inicioEspera > 0) {
            
            long tempoDecorrido = System.currentTimeMillis() - inicioEspera;

            if (tempoDecorrido >= LIMITE_ESPERA_MS) {
                String sentido = isHorizontal ? "HORIZONTAL" : "VERTICAL";
                System.out.println("[ALERTA] Cruzamento " + cruzamento.getId() + " - Sentido " + sentido + ": espera excedida (10s)!");

                // Cria e envia a mensagem CruzamentoAlerta (SEM PRIORIDADE)
                CruzamentoAlerta alerta = new CruzamentoAlerta();
                alerta.setIdCruzamento(cruzamento.getId());
                alerta.setMensagem("Tempo de espera excedido no sentido " + sentido + ". Inversão de sinal solicitada.");
                alerta.setTempoEsperaSegundos((int) (tempoDecorrido / 1000));
                // O campo 'prioridade' foi removido conforme sua solicitação

                producer.enviarAlerta(alerta);

                // Reinicia a contagem de tempo para este semáforo, para evitar spam de alertas
                long novoTimestamp = System.currentTimeMillis();
                if (isHorizontal) {
                    cruzamento.setInicioEsperaHorizontal(novoTimestamp);
                } else {
                    cruzamento.setInicioEsperaVertical(novoTimestamp);
                }
                repository.save(cruzamento);
            }
        } else if (statusSinal == StatusSinal.VERDE || !temCarroNaFila) { 
            // Se o sinal abriu ou a fila zerou, garante que o contador de espera zere.
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


}