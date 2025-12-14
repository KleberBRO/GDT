package com.gestortransito.modulos.contratos.mensagens;

public class SensorVeiculo {

    private String idVeiculo;
    private String idVia;
    private long timestamp;

    public String getIdVeiculo() {
        return idVeiculo;
    }

    public void setIdVeiculo(String idVeiculo) {
        this.idVeiculo = idVeiculo;
    }

    public String getIdVia() {
        return idVia;
    }

    public void setIdVia(String idVia) {
        this.idVia = idVia;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}