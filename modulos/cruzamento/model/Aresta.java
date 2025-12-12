import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Aresta {
    private String de;      // O ID do nó (cruzamento) de onde a via sai
    private String para;    // O ID do nó (cruzamento) para onde a via vai
    // private int peso;    // Opcional: Se a via tiver um tempo de viagem (peso)
}