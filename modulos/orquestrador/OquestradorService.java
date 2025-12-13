@Service
public class OrquestradorService{

    private final CruzamentoRepository repository;
    private final OrquestradorProducer orquestrador;


   public tratarStatus(String idCruzamento, String status){
        //acha o cruzamento
       Cruzamento cruzamento = repository.findById(idCruzamentoAlvo)
               .orElseThrow(() -> new RuntimeException("Cruzamento alvo não encontrado: " + idCruzamentoAlvo));


   }

   //com o alerta, ele manda o comando para o cruzamento fechar
    public tratarAlerta(String idCruzamento, String alerta){
        //acha o cruzamento
        Cruzamento cruzamento = repository.findById(idCruzamentoAlvo)
                .orElseThrow(() -> new RuntimeException("Cruzamento alvo não encontrado: " + idCruzamentoAlvo));

        OrquestradorComando comando;
        //verificar o alerta, se o alerta for de prioridade alta, manda o comando de abrir
        if (alerta.toUpperCase().equals("ALTA")){
            System.err.println("ALERTA RECEBIDO, CRUZAMENTO DE ID:" + idCruzamento + " está fechado por tempo demais, enviando comando para abri-lo.");
            comando= = OrquestradorComandos.ABRIR;
        }else{
            System.err.println("ALERTA RECEBIDO MAS SEM NECESSIDADE PELO CRUZAMENTO DE ID:" + idCruzamento + ". Portanto, permanecerá fechado.");
            comando= = OrquestradorComandos.FECHAR;
        }

        //envia para o produtor de comando
        enviarComando(comando);
    }



}