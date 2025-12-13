import React, { useRef } from 'react';

export default function SimulationControls({ 
  qtdVeiculos, 
  setQtdVeiculos, 
  addIntersection, 
  toggleSimulation, 
  isSimulating,
  exportGraph,
  importGraph
}) {
  const fileInputRef = useRef(null);

  return (
    <div className='controls-container'>
      <h3>Controle da Simulação</h3>
      <div>
        <label>Qtd Veículos:</label>
        <input 
          type="number" 
          value={qtdVeiculos} 
          onChange={(e) => setQtdVeiculos(e.target.value)} 
          style={{ width: '60px', marginLeft: '5px' }} 
          min="1" 
        />
      </div>
      <button 
        onClick={addIntersection} 
        disabled={isSimulating} 
        className="control-button btn-add"
      >
        + Add Cruzamento
      </button>
      <button 
        onClick={toggleSimulation} 
        className={`control-button ${isSimulating ? 'btn-stop' : ''}`}
      >
        {isSimulating ? "Parar Simulação" : "Iniciar Simulação"}
      </button>

      {/* Botões de Importar/Exportar */}
      <div style={{ display: 'flex', gap: '5px', flexDirection: 'column', marginTop: '10px' }}>
        <button onClick={exportGraph} className="control-button" disabled={isSimulating}>
          💾 Salvar Grafo
        </button>
        
        <button 
          onClick={() => fileInputRef.current.click()} 
          className="control-button" 
          disabled={isSimulating}
          style={{ backgroundColor: '#6c757d' }} // Cor diferente opcional
        >
          BsFolderOpen Carregar Grafo
        </button>
        
        {/* Input oculto para selecionar o arquivo */}
        <input 
          type="file" 
          ref={fileInputRef}
          style={{ display: 'none' }} 
          accept=".json"
          onChange={importGraph}
        />
      </div>

      <hr style={{width: '100%', margin: '10px 0'}}/>

      <div>
        <h4>dicas</h4>
        <p>Use backspace para deletar nós e arestas</p>
        <p>preto = origem</p>
        <p>azul = destino</p>
      </div>
    </div>
  );
}