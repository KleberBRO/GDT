// modulos/cruzamento/src/main/java/CruzamentoRepository.java
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// Estende JpaRepository<[Classe do Modelo], [Tipo da Chave Primária]>
@Repository
public interface CruzamentoRepository extends JpaRepository<Cruzamento, String> {
    // O Spring automaticamente cria funções como findById, findAll, save, delete, etc.
}