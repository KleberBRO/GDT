
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SistemaConfig {
    private String tipoEvento;      // "INICIAR_SIMULACAO" ou "ATUALIZAR_PARAMETROS"
    private DadosGrafo dadosGrafo;   // O objeto que contém a estrutura do mapa
    private int qtdVeiculos;        // Número inicial de veículos para o Simulador
}