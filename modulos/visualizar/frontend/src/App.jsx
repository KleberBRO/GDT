import React, { useState, useCallback } from 'react';
import ReactFlow, { 
  addEdge, 
  Background, 
  Controls, 
  applyEdgeChanges, 
  applyNodeChanges,
  ReactFlowProvider
} from 'reactflow';
import 'reactflow/dist/style.css';

import TrafficLightNode from './components/NoCruzamento';

// Registrar o tipo de nó personalizado
const nodeTypes = { trafficLight: TrafficLightNode };

const initialNodes = [
  { 
    id: '1', 
    type: 'trafficLight', 
    position: { x: 250, y: 5 }, 
    data: { id: '1', status: 'VERMELHO', queueCount: 5 } 
  },
];

const initialEdges = [];

export default function App() {
  const [nodes, setNodes] = useState(initialNodes);
  const [edges, setEdges] = useState(initialEdges);

  // Funções padrão do React Flow para mover nós e conectar arestas
  const onNodesChange = useCallback(
    (changes) => setNodes((nds) => applyNodeChanges(changes, nds)),
    []
  );
  const onEdgesChange = useCallback(
    (changes) => setEdges((eds) => applyEdgeChanges(changes, eds)),
    []
  );
  
  // Conectar duas vias (Cria uma aresta no grafo)
  const onConnect = useCallback(
    (params) => setEdges((eds) => addEdge({ ...params, animated: true, label: 'Via' }, eds)),
    []
  );

  // Função para adicionar novo cruzamento (Requisito: adicionar novos cruzamentos )
  const addIntersection = () => {
    const newId = (nodes.length + 1).toString();
    const newNode = {
      id: newId,
      type: 'trafficLight',
      position: { x: Math.random() * 400, y: Math.random() * 400 },
      data: { id: newId, status: 'VERMELHO', queueCount: 0 },
    };
    setNodes((nds) => nds.concat(newNode));
  };

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      {/* Barra de Ferramentas / Controles de Simulação */}
      <div style={{ position: 'absolute', zIndex: 10, padding: 10, background: 'rgba(255,255,255,0.8)' }}>
        <h3>Controle da Simulação</h3>
        <button onClick={addIntersection}>+ Adicionar Cruzamento</button>
        {/* Aqui você adicionaria controles para enviar ao Kafka via FastAPI  */}
      </div>

      {/* Wrap com ReactFlowProvider para garantir o contexto */}
      <ReactFlowProvider>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          fitView
        >
          <Background />
          <Controls />
        </ReactFlow>
      </ReactFlowProvider>
    </div>
  );
}
// ...existing code...