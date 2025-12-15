package com.gestortransito.modulos.contratos.mensagens;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Contém a topologia da rede (o grafo) que será enviada dentro de SistemaConfig.
 * Esta classe é necessária para representar a estrutura do mapa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DadosGrafo {
    
    // Lista de todos os IDs de Cruzamentos na rede (ex: "cruzamento_1", "cruzamento_2")
    private List<String> cruzamentos; 
    
    // Mapeamento de todas as Arestas (Vias) no sistema
    // Chave: ID do cruzamento de origem (Ex: "cruzamento_1")
    // Valor: Lista de conexões que partem desse cruzamento
    private Map<String, List<ConexaoVia>> arestas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConexaoVia {
        private String destinoId;
        private String idViaIda;    // Via que sai da Origem e vai para o Destino
        private String idViaVolta;  // Via que sai do Destino e volta para a Origem
        private String sentido;     // "Norte-Sul" ou "Leste-Oeste"
    }

}
