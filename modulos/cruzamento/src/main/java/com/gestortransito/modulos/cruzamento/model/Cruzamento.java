package com.gestortransito.modulos.cruzamento.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;

import com.gestortransito.modulos.cruzamento.enums.StatusSinal;


@Entity
@Data
@NoArgsConstructor
public class Cruzamento {

    @Id
    private String id;
    
    // Mapeamento da fila de veículos por ID da via (Ex: "via_Vizinho2_para_X_Norte-Sul" -> 5 carros)
    @ElementCollection
    private Map<String, Integer> filasPorVia;

    // Semáforo para o sentido Horizontal (Leste-Oeste)
    @Enumerated(EnumType.STRING)
    private StatusSinal statusSinalHorizontal;

    // Semáforo para o sentido Vertical (Norte-Sul)
    @Enumerated(EnumType.STRING)
    private StatusSinal statusSinalVertical;

    // Timestamp de quando o PRIMEIRO carro parou no sentido HORIZONTAL (para iniciar o timer)
    private long inicioEsperaHorizontal;

    // Timestamp de quando o PRIMEIRO carro parou no sentido VERTICAL (para iniciar o timer)
    private long inicioEsperaVertical;

    /**
     * Construtor para inicialização de um novo cruzamento.
     * Inicializa com um conjunto de vias mock para simulação.
     */
    public Cruzamento(String id) {
        this.id = id;
        this.filasPorVia = new HashMap<>();
        
        // Exemplo de inicialização de vias de MÃO DUPLA (4 vias de chegada/fila para este cruzamento)
        
        // Vias Verticais (Norte-Sul - sentindo para este cruzamento)
        filasPorVia.put("via_" + id + "_para_VizinhoNorte_Norte-Sul", 0); // Vindo de cima/Norte
        filasPorVia.put("via_VizinhoSul_para_" + id + "_Norte-Sul", 0); // Vindo de baixo/Sul
        
        // Vias Horizontais (Leste-Oeste - sentindo para este cruzamento)
        filasPorVia.put("via_" + id + "_para_VizinhoLeste_Leste-Oeste", 0); // Vindo da direita/Leste
        filasPorVia.put("via_VizinhoOeste_para_" + id + "_Leste-Oeste", 0); // Vindo da esquerda/Oeste

        // Estado inicial alternado
        this.statusSinalHorizontal = StatusSinal.VERDE;
        this.statusSinalVertical = StatusSinal.VERMELHO;
        this.inicioEsperaHorizontal = 0;
        this.inicioEsperaVertical = 0;
    }

    /**
     * Identifica a que semáforo a via pertence (Vertical ou Horizontal) 
     * com base na sua nova convenção de nomenclatura.
     */
    public boolean isViaHorizontal(String idVia) {
        // Assume que a string "LESTE-OESTE" define o sentido horizontal
        return idVia.toUpperCase().contains("LESTE-OESTE");
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