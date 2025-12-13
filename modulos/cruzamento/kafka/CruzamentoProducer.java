//Produtores recebem @Service porque eles contêm a regra de negócio de enviar/processar dados.
@Service
public class CruzamentoProducer {

    // Injeta o template do Kafka usar os recursos da ferramente e enviar mensagens
    //vai fornecer o método send() para publicar mensagens
    //key= garante que todas as mensagens de um mesmo recurso caiam na mesma partição do Kafka
    //Object = mensagem em si
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CruzamentoProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    } //construtor


    //envia a mensgagem do tipo CruzamentoStatus do semáforo para o orquestrador, no tópico cruzamento.telemetria declarado no send
    public void enviarStatus(CruzamentoStatus status) {
        // A chave (key) da mensagem deve ser o ID do cruzamento (importante para particionamento)
        String key = status.getIdCruzamento();

        //enviar dados ao Kafka
        //cruzamento.telemetria = O nome do tópico para onde a mensagem será enviada
        //key = O ID do cruzamento
        //status= O objeto CruzamentoStatus
        kafkaTemplate.send("cruzamento.telemetria", key, status) //<-----------------------------------------------implementar este tópico para o orquestrador comsumir isso [implementar orquetsrador consumidor]
                .whenComplete((result, ex) -> {
                    if (ex == null) { //ex é uma exceção que pode ocorrer no send
                        System.out.println("Status enviado! Cruzamento: " + key);
                    } else {
                        System.err.println("Erro ao enviar status: " + ex.getMessage());
                    }
                });
    }

    //envia alerta de uma espera longa no tópico cruzamento.alerta para o orquestrador
    public void enviarAlerta(CruzamentoAlerta alerta) {
        String idCruzamento= alerta.getIdCruzamento();
        kafkaTemplate.send("cruzamento.alerta", idCruzamento, alerta); //<-----------------------------------------------implementar este tópico para o orquestrador comsumir isso [implementar orquetsrador consumidor]
        System.out.println("Alerta de espera longa para: " + idCruzamento);
    }
}