@Component
public class OrquestradorConsumer {
    private final OrquestradorService orquestradorService;

    public OrquestradorConsumer(OrquestradorService orquestradorService) {
        this.orquestradorService = orquestradorService;
    } //construtor

  
    //receber alertas para um comando imediato no tópico cruzamento.alerta
    @KafkaListener(topics = "cruzamento.alerta", groupId = "cruzamento-group")
    public void handleCruzamentoAlerta(CruzamentoAlerta alerta) {
        System.out.println("Alerta do cruzamento de id " + alerta.getIdCruzamento() + " recebido pelo oquestrador. Prioridade: " + alerta.getPrioridade());
        orquestradorService.tratarAlerta(alerta); // Lógica de tratamento
    }

    // NOVO: Recebe a configuração do grafo do front/módulo Visualizar
    @KafkaListener(topics = "sistema.configuracao", groupId = "orquestrador-config-group")
    public void handleSistemaConfig(SistemaConfig config) {
        System.out.println("Configuração do grafo ('sistema.configuracao') recebida.");
        orquestradorService.configurarSistema(config);
    }
}