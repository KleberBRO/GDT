import React from 'react';

export default function SimulationControls({ 
  qtdVeiculos, 
  setQtdVeiculos, 
  addIntersection, 
  toggleSimulation, 
  isSimulating 
}) {
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
      <div>
        <h4>dicas</h4>
        <p>Use backspace para deletar nós e arestas</p>
        <p>preto = origem</p>
        <p>azul = destino</p>
      </div>
    </div>
  );
}