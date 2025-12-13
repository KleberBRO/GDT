public class CruzamentoStatus {
    private String idCruzamento;
    private String statusSinal;
    private int tamanhoFila;
    private long timestamp;

    public String getIdCruzamento() {
        return idCruzamento;
    }

    public void setIdCruzamento(String idCruzamento) {
        this.idCruzamento = idCruzamento;
    }

    public String getStatusSinal() {
        return statusSinal;
    }

    public void setStatusSinal(String statusSinal) {
        this.statusSinal = statusSinal;
    }

    public int getTamanhoFila() {
        return tamanhoFila;
    }

    public void setTamanhoFila(int tamanhoFila) {
        this.tamanhoFila = tamanhoFila;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}