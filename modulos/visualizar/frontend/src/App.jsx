import React, { useState, useCallback, useEffect } from 'react';
import ReactFlow, { 
  addEdge, 
  Background, 
  Controls, 
  applyEdgeChanges, 
  applyNodeChanges,
  ReactFlowProvider,
  getConnectedEdges
} from 'reactflow';
import 'reactflow/dist/style.css';

import NoCruzamento from './components/NoCruzamento';
import CustomEdge from './components/CustomEdge';

// Registrar o tipo de nó personalizado
const nodeTypes = { trafficLight: NoCruzamento };
const edgeTypes = { custom: CustomEdge };

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

  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8000/ws');

    ws.onopen = () => {
      console.log('Conexão WebSocket estabelecida');
    };

    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        handleSocketMessage(message);
      } catch (error) {
        console.error('Erro ao processar mensagem WebSocket:', error);
      }
    };

    ws.onclose = () => {
      console.log('Conexão WebSocket fechada');
    };

    return () => {
      ws.close();
    };
  }, []);

  const handleSocketMessage = (msg) => {
    const { topic, data } = msg;

    if (topic === 'cruzamento.status') {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === data.id_cruzamento) {
            let visualStatus = 'VERMELHO';
            if (data.status === 'N-S') visualStatus = 'VERDE';
            else if (data.status === 'L-O') visualStatus = 'VERDER_H';

            return {
              ...node,
              data: {
                ...node.data,
                status: visualStatus,
                queueCount: data.tamanho_fila || node.data.queueCount,
                longwaitAler: (data.tamanho_fila > 10)
              }
            };
          }
          return node;
        })
      );
    }
  };

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
      (params) => setEdges((eds) => addEdge({ 
        ...params, 
        type: 'customEdge', // Define que usará nossa aresta personalizada
        animated: true, 
        data: { queueCount: 0 } // Inicia a fila zerada na aresta
      }, eds)),
      []
  );

  const onNodesDelete = useCallback(
    (deleted) => {
      setEdges((eds) => {
        const connectedEdges = getConnectedEdges(deleted, eds);
        return eds.filter(
          (e) => !connectedEdges.some((ce) => ce.id === e.id)
        );
      });
    },
    []
  );

  const onEdgeDoubleClick = useCallback((event, edge) => {
    setEdges((eds) => eds.filter((e) => e.id !== edge.id));
  }, []);

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

  const startSimulation = async () => {
    const payload = {
      tipo_evento: 'INICIAR_SIMULACAO',
      qtd_veiculos: 50,
      dados_grafo: {
        nodos: nodes.map(n => n.id),
        arestas: edges.map(e => '${e.source}-${e.target}')
      }
    };

    try {
      await fetch('http://localhost:8000/configurar-simulacao', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      alert('Simulação iniciada com sucesso!');
    } catch (error) {
      console.error('Erro ao iniciar simulação:', error);
      alert('Falha ao iniciar simulação.');
    }
  }; 

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      {/* Barra de Ferramentas / Controles de Simulação */}
      <div style={{ position: 'absolute', zIndex: 10, padding: 10, background: 'rgba(255,255,255,0.8)' }}>
        <h3>Controle da Simulação</h3>
        <button onClick={addIntersection}>+ Adicionar Cruzamento</button>
        <button onClick={startSimulation}>Iniciar Simulação</button>
      </div>

      <ReactFlowProvider>
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onNodesDelete={onNodesDelete}
          onEdgeDoubleClick={onEdgeDoubleClick}
          onEdgesChange={onEdgesChange}
          onConnect={onConnect}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          fitView
        >
          <Background />
          <Controls />
        </ReactFlow>
      </ReactFlowProvider>
    </div>
  );
}