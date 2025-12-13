@Service
public class OrquestradorProducer{

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrquestradorProducer(KafkaTemplate<String, Object> kafkaTemplate){
        this.kafkaTemplate = kafkaTemplate;
    }

    //msg para o cruzador,o comando, no tópico orquestrador.comando
    public void enviarComando(OrquestradorComando comando){
        String key = comando.getIdCruzamentoAlvo();
        kafkaTemplate.send("orquestrador.comando", key, comando);
        System.out.println("Comando "+ comando.comando +" para o cruzamento de id: " + idCruzamento);

    }

    //evento para o Visualisar no tópico visualizar.evento
    public void enviarEvento(OrquestradorEvento evento){

    }


}