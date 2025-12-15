package com.gestortransito.modulos.contratos.mensagens;

public class CruzamentoAlerta {

    private String idCruzamento;
    private String mensagem;
    private int tempoEsperaSegundos;
    private String sentidoComProblema; // HORIZONTAL ou VERTICAL

    public String getIdCruzamento() {
        return idCruzamento;
    }

    public void setIdCruzamento(String idCruzamento) {
        this.idCruzamento = idCruzamento;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public int getTempoEsperaSegundos() {
        return tempoEsperaSegundos;
    }

    public void setTempoEsperaSegundos(int tempoEsperaSegundos) {
        this.tempoEsperaSegundos = tempoEsperaSegundos;
    }

    public String getSentidoComProblema() {
        return sentidoComProblema;
    }

    public void setSentidoComProblema(String sentidoComProblema) {
        this.sentidoComProblema = sentidoComProblema;
    }

}
