@Component
public class OrquestradorConsumer {
    private final OrquestradorService orquestradorService;

    public OrquestradorConsumer(OrquestradorService orquestradorService) {
        this.orquestradorService = orquestradorService;
    } //construtor

    //receber os status do cruzamento e gerar um comando no tópico cruzamento.telemetria
    @KafkaListener(topics = "cruzamento.telemetria", groupId = "cruzamento-group")
    public void handleCruzamentoStatus(CruzamentoStatus status) {
        System.out.println("Status do cruzamento de id " + status.getIdCruzamento() + " recebido pelo oquestrador));
        cruzamentoService.tratarStatus(); //CRIAR LÓGICA
    }

    //receber alertas para um comando imediato no tópico cruzamento.alerta
    @KafkaListener(topics = "cruzamento.alerta", groupId = "cruzamento-group")
    public void handleCruzamentoAlerta(CruzamentoAlerta alerta) {
        System.out.println("Alerta do cruzamento de id " + alerta.getIdCruzamento() + " recebido pelo oquestrador));
                cruzamentoService.tratarAlerta(); //CRIAR LÓGICA
    }
}