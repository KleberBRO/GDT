package com.gestortransito.modulos.contratos.mensagens;
public class OrquestradorComando {
    private String idCruzamentoAlvo;
    private String comando; //abrir ou fechar
    private String idTransacao;

    public String getIdCruzamentoAlvo() {
        return idCruzamentoAlvo;
    }

    public void setIdCruzamentoAlvo(String idCruzamentoAlvo) {
        this.idCruzamentoAlvo = idCruzamentoAlvo;
    }

    public String getComando() {
        return comando;
    }

    public void setComando(String comando) {
        this.comando = comando;
    }

    public String getIdTransacao() {
        return idTransacao;
    }

    public void setIdTransacao(String idTransacao) {
        this.idTransacao = idTransacao;
    }
}