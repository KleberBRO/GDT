package com.gestortransito.modulos.contratos.mensagens;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class CruzamentoConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("filasPorVia")
    private Map<String, Integer> filasPorVia;

    @JsonProperty("statusSinalHorizontal")
    private String statusSinalHorizontal;

    @JsonProperty("statusSinalVertical")
    private String statusSinalVertical;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Integer> getFilasPorVia() {
        return filasPorVia;
    }

    public void setFilasPorVia(Map<String, Integer> filasPorVia) {
        this.filasPorVia = filasPorVia;
    }

    public String getStatusSinalHorizontal() {
        return statusSinalHorizontal;
    }

    public void setStatusSinalHorizontal(String statusSinalHorizontal) {
        this.statusSinalHorizontal = statusSinalHorizontal;
    }

    public String getStatusSinalVertical() {
        return statusSinalVertical;
    }

    public void setStatusSinalVertical(String statusSinalVertical) {
        this.statusSinalVertical = statusSinalVertical;
    }
}
