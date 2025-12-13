@Service
public class CruzamentoService {

    private final CruzamentoRepository repository;
    private final CruzamentoProducer producer;

    public CruzamentoService(CruzamentoProducer producer, CruzamentoRepository repository) {
        this.producer = producer;
        this.repository = repository;
    }

    //quando um veículo chega no cruzamento
    public void adicionarVeiculoNaFila(String idVia, String idCruzamento) {
        //Busca o cruzamento
        Cruzamento cruzamento = repository.findById(idCruzamento)
                .orElseThrow(() -> new RuntimeException("Cruzamento não encontrado: " + idCruzamento)); // [cite: 96] (Referência)

        //atualiza a fila da via específica
        // Usa getOrDefault para obter o valor atual ou 0 se a via não existir no Map ainda.
        int filaAtual = cruzamento.getFilasPorVia().getOrDefault(idVia, 0);
        cruzamento.getFilasPorVia().put(idVia, filaAtual + 1); // Incrementa o contador da via

        // 3. Lógica de início de espera (se for o primeiro carro a chegar E o sinal estiver VERMELHO)
        if (filaAtual == 0 && cruzamento.getStatusSinal() == StatusSinal.VERMELHO && cruzamento.getInicioEspera() == 0) {
            cruzamento.setInicioEspera(System.currentTimeMillis()); //isso vai refletir em iniciarMonitoramento()
        }

        repository.save(cruzamento);  // Salva a alteração
        enviarStatusAtual(cruzamento); // Envia o status atual do cruzamento
    }

   //segue o comando do orquestrador de abrir ou fechar
    public void executarComando(String idCruzamentoAlvo, String comando) {

        // busca o cruzamento
        Cruzamento cruzamento = repository.findById(idCruzamentoAlvo)
                .orElseThrow(() -> new RuntimeException("Cruzamento alvo não encontrado: " + idCruzamentoAlvo));

        StatusSinal novoStatus;
        if (comando.toUpperCase().equals("ABRIR")) {
            novoStatus = StatusSinal.VERDE;
        } else if (comando.toUpperCase().equals("FECHAR")) {
            novoStatus = StatusSinal.VERMELHO;
        } else {
            System.err.println("Comando desconhecido recebido: " + comando);
            return;
        }

        cruzamento.setStatusSinal(novoStatus); //atualiza

        // Se o sinal abriu, zera a fila de todas as vias e o contador de espera
        if (novoStatus == StatusSinal.VERDE) {
            cruzamento.getFilasPorVia().clear(); // Zera o Map inteiro
            cruzamento.setInicioEspera(0);       // Zera o contador de espera
        }

        repository.save(cruzamento); //salva
        enviarStatusAtual(cruzamento); //envia pro produtor que manda ao Orquestrador

        System.out.println("Comando executado: " + comando + " para o cruzamento " + idCruzamentoAlvo + ". Novo status: " + novoStatus);
    }

    //Recebe a entidade Cruzamento para enviar dados de telemetria corretos.
    private void enviarStatusAtual(Cruzamento cruzamento) {

        CruzamentoStatus status = new CruzamentoStatus();
        status.setIdCruzamento(cruzamento.getId()); // Usa o ID real do cruzamento [cite: 105] (Referência)
        status.setStatusSinal(cruzamento.getStatusSinal().name()); // Usa o status real (VERDE/VERMELHO) [cite: 100] (Referência)

        // Calcula o tamanho total da fila (soma de todas as filas por via)
        int tamanhoTotalFila = cruzamento.getFilasPorVia().values().stream()
                .mapToInt(Integer::intValue)
                .sum();

        status.setTamanhoFila(tamanhoTotalFila); // Envia o tamanho total da fila
        status.setTimestamp(System.currentTimeMillis() / 1000); // [cite: 105] (Referência)

        producer.enviarStatus(status); // [cite: 106] (Referência)
    }

    // Inicia um loop para checar alertas periodicamente e enviar status
    //executa a cada 5 segundos para checar o limite de 1 minuto.
    @Scheduled(fixedRate = 5000) // 5000ms = 5 segundos
    private void iniciarMonitoramento() {
        //itera sobre todos os cruzamentos gerenciados pelo módulo
        repository.findAll().forEach(cruzamento -> {

            //pega os atributos do cruzamento da vez
            int tamanhoFila = cruzamento.getTamanhoFila();
            String statusSinal = cruzamento.getStatusSinal().toString(); // Usando enum
            long inicioEspera = cruzamento.getInicioEspera();

            // se tiver veículo na fila e o sinal está VERMELHO...
            if (tamanhoFila > 0 && statusSinal.equals(StatusSinal.VERMELHO.name())) {

                long tempoDecorrido = System.currentTimeMillis() - inicioEspera; //calcula o tempo de demora
                final long LIMITE_ESPERA_MS = 60 * 1000; // 1 minuto

                if (tempoDecorrido >= LIMITE_ESPERA_MS) { //se bater este 1 minuto, emite alerta

                    System.out.println("[ALERTA] Cruzamento " + cruzamento.getId() + ": espera excedida!");

                    // cria a mensagem CruzamentoAlerta e manda o produtor enviar
                    CruzamentoAlerta alerta = new CruzamentoAlerta();
                    alerta.setIdCruzamento(cruzamento.getId());
                    alerta.setMensagem("Tempo de espera excedido: Prioridade na mudança de sinal.");
                    alerta.setTempoEsperaSegundos((int) (tempoDecorrido / 1000));
                    alerta.setPrioridade("ALTA"); //<--------------------------------------------------------------------lembrar de levar em conta estes status de prioridade ao fazer o Orquestrador

                    producer.enviarAlerta(alerta);

                    cruzamento.setInicioEspera(System.currentTimeMillis());//reinicia a contágem de tempo
                    repository.save(cruzamento); // Salva a mudança no banco (JPA)
                }
            } else { //se o sinal está verde, não conta tempo e zera o tempo que estava contando para ser ativado só quando tiver um veículo denovo
                // Se o sinal abriu ou a fila zerou, garante que o contador de espera zere.
                if (cruzamento.getInicioEspera() != 0) {
                    cruzamento.setInicioEspera(0);
                    repository.save(cruzamento);
                }
            }
        });
    }
}