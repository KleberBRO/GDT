import jakarta.persistence.*;
import lombok.Data;

@Entity
@Lombok
@Data
public class Cruzamento {

    @Id
    private String id; // Ex: "cruzamento_01"

    // Mapeamento da fila de veículos por ID da via (Ex: "via_A_norte" -> 5 carros)
    // Isso é mais robusto do que ter apenas um tamanhoFila
    @ElementCollection // Para armazenar um Map como parte da entidade
    private Map<String, Integer> filasPorVia;

    @Enumerated(EnumType.STRING) // Salva o nome da enum no banco (VERDE, VERMELHO)
    private StatusSinal statusSinal;

    // Usado para o monitoramento de 1 minuto
    private long inicioEspera;

    // Construtor, Getters e Setters

    // Construtor para inicialização
    public Cruzamento(String id) {
        this.id = id;
        this.filasPorVia = new HashMap<>();
        this.statusSinal = StatusSinal.VERMELHO; // Inicia sempre no vermelho
        this.inicioEspera = 0;
    }
    // Construtor default
    public Cruzamento() {}
}