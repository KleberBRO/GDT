package com.gestortransito.modulos.contratos.mensagens;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DadosGrafoSimples {

    @JsonProperty("nodos")
    private List<String> nodos;

    @JsonProperty("arestas")
    private List<String> arestas;

    public List<String> getNodos() {
        return nodos;
    }

    public void setNodos(List<String> nodos) {
        this.nodos = nodos;
    }

    public List<String> getArestas() {
        return arestas;
    }

    public void setArestas(List<String> arestas) {
        this.arestas = arestas;
    }
}
