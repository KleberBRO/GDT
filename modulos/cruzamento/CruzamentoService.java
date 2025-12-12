@Service
public class CruzamentoService {

    private final CruzamentoProducer producer;
    private int tamanhoFila = 0;
    private String statusSinal = "VERMELHO"; // Status inicial

    public CruzamentoService(CruzamentoProducer producer) {
        this.producer = producer;
        iniciarMonitoramento();// Inicia um loop para checar alertas periodicamente e enviar status
    }

    //quando um veículo chega no cruzamento
    //ATUALIZAR A ADIÇÃO, ACHO QUE NÃO ENTENDEU
    public void adicionarVeiculoNaFila(String idVia) {
        // busca o cruzamento pela id
        Cruzamento cruzamento = repository.findById(idCruzamento)
                .orElseThrow(() -> new RuntimeException("Cruzamento não encontrado: " + idCruzamento));

        // 2. Atualiza a fila da via
        int filaAtual = cruzamento.getFilasPorVia().getOrDefault(idVia, 0);
        cruzamento.getFilasPorVia().put(idVia, filaAtual + 1);

        // 3. Lógica de início de espera (se a fila estava vazia e o sinal está vermelho)
        if (filaAtual == 0 && cruzamento.getStatusSinal() == StatusSinal.VERMELHO && cruzamento.getInicioEspera() == 0) {
            cruzamento.setInicioEspera(System.currentTimeMillis());
        }

        repository.save(cruzamento);  // salva a alteração
        enviarStatusAtual();// Toda vez que a fila muda, o status deve ser enviado
    }

   //segue o comando do rquestrador de abrir ou fechar
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

    //ATUALIZAR ANTES PARA PODER RECEBER O CRUZAMENTO
    private void enviarStatusAtual(Cruzamento cruzamento) {

        CruzamentoStatus status = new CruzamentoStatus();
        status.setIdCruzamento("cruzamento_01"); // mudar se houver vários cruzamentos
        status.setStatusSinal(this.statusSinal);
        status.setTamanhoFila(this.tamanhoFila);
        status.setTimestamp(System.currentTimeMillis() / 1000);

        producer.enviarStatus(status); //chama o produtor para gerar a mensagem que vai ser enviada para o Orquestrador
    }


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