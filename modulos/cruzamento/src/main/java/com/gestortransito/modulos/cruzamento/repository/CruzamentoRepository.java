package com.gestortransito.modulos.cruzamento.repository;

import com.gestortransito.modulos.cruzamento.model.Cruzamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Estende JpaRepository<[Classe do Modelo], [Tipo da Chave Primária]>
@Repository
public interface CruzamentoRepository extends JpaRepository<Cruzamento, String> {
    // O Spring automaticamente cria funções como findById, findAll, save, delete, etc.
}