package com.gestortransito.modulos.contratos.mensagens;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SistemaConfigSimplificado {

    @JsonProperty("tipo_evento")
    private String tipoEvento;

    @JsonProperty("qtd_veiculos")
    private int qtdVeiculos;

    @JsonProperty("dados_grafo")
    private DadosGrafoSimples dadosGrafo;

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public int getQtdVeiculos() {
        return qtdVeiculos;
    }

    public void setQtdVeiculos(int qtdVeiculos) {
        this.qtdVeiculos = qtdVeiculos;
    }

    public DadosGrafoSimples getDadosGrafo() {
        return dadosGrafo;
    }

    public void setDadosGrafo(DadosGrafoSimples dadosGrafo) {
        this.dadosGrafo = dadosGrafo;
    }
}
