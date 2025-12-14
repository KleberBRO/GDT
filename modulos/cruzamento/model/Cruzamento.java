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

   // Semáforo para o sentido Horizontal (Leste-Oeste)
    @Enumerated(EnumType.STRING)
    private StatusSinal statusSinalHorizontal;

    // Semáforo para o sentido Vertical (Norte-Sul)
    @Enumerated(EnumType.STRING)
    private StatusSinal statusSinalVertical;

    // Usado para o monitoramento de 10 segundos para o sentido HORIZONTAL
    // Armazena o timestamp de quando o PRIMEIRO carro parou no sentido HORIZONTAL
    private long inicioEsperaHorizontal;

    // Usado para o monitoramento de 10 segundos para o sentido VERTICAL
    // Armazena o timestamp de quando o PRIMEIRO carro parou no sentido VERTICAL
    private long inicioEsperaVertical;
    

    // Construtor default
    public Cruzamento() {}

    /**
     * Identifica a que semáforo a via pertence (Vertical ou Horizontal).
     * Esta é uma função de 'mock' e deve ser adaptada à sua convenção de IDs de via.
     */
    public boolean isViaHorizontal(String idVia) {
        // Exemplo: vias com "OESTE" ou "LESTE" são horizontais
        return idVia.toUpperCase().contains("OESTE") || idVia.toUpperCase().contains("LESTE");
    }

    /**
     * Obtém o status do semáforo que a via deve seguir.
     */
    public StatusSinal getStatusSinalParaVia(String idVia) {
        if (isViaHorizontal(idVia)) {
            return statusSinalHorizontal;
        } else {
            return statusSinalVertical;
        }
    }

}