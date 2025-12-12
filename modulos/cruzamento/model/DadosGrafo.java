import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DadosGrafo {
    private List<String> nodos; // Lista dos IDs dos cruzamentos (ex: "cruzamento_01")
    private List<Aresta> arestas; // Lista das vias/ligações entre os cruzamentos
}