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
import './App.css';

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
  const [qtdVeiculos, setQtdVeiculos] = useState(50);
  const [isSimulating, setIsSimulating] = useState(false);

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
                longwaitAlert: (data.tamanho_fila > 10)
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
      (params) => {
        // Bloqueia conexão manual se estiver simulando (redundante com props, mas seguro)
        if (isSimulating) return; 
        setEdges((eds) => addEdge({ 
          ...params, 
          type: 'custom', 
          animated: true, 
          data: { queueCount: 0 } 
        }, eds));
      },
      [isSimulating]
  );

  const onNodesDelete = useCallback(
    (deleted) => {
      if (isSimulating) return;
      setEdges((eds) => {
        const connectedEdges = getConnectedEdges(deleted, eds);
        return eds.filter(
          (e) => !connectedEdges.some((ce) => ce.id === e.id)
        );
      });
    },
    [isSimulating]
  );

  const onEdgeDoubleClick = useCallback((event, edge) => {
    if (isSimulating) return; // Bloqueia deleção por duplo clique durante simulação
    setEdges((eds) => eds.filter((e) => e.id !== edge.id));
  }, [isSimulating]);

  // Função para adicionar novo cruzamento (Requisito: adicionar novos cruzamentos )
  const addIntersection = () => {
    if (isSimulating) return;

    const newId = (nodes.length + 1).toString();
    const newNode = {
      id: newId,
      type: 'trafficLight',
      position: { x: Math.random() * 400, y: Math.random() * 400 },
      data: { id: newId, status: 'VERMELHO', queueCount: 0 },
    };
    setNodes((nds) => nds.concat(newNode));
  };

const toggleSimulation = async () => {
    // Define o tipo de evento baseado no estado atual
    const eventType = isSimulating ? 'PARAR_SIMULACAO' : 'INICIAR_SIMULACAO';
    
    const payload = {
      tipo_evento: eventType,
      qtd_veiculos: isSimulating ? 0 : 50, // Se estiver parando, zeramos ou ignoramos
      dados_grafo: {
        nodos: nodes.map(n => n.id),
        // Correção importante: uso de crase (`) para template string
        arestas: edges.map(e => `${e.source}-${e.target}`) 
      }
    };

    try {
      // O Backend enviará este payload para o Kafka (tópico sistema.configuracao)
      await fetch('http://localhost:8000/configurar-simulacao', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      // Atualiza o estado e avisa o usuário
      if (isSimulating) {
        setIsSimulating(false);
      } else {
        setIsSimulating(true);
      }

    } catch (error) {
      console.error('Erro ao comunicar com simulação:', error);
      alert('Falha ao comunicar com o servidor.');
    }
  };

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      {/* Barra de Ferramentas / Controles de Simulação */}
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
        >+ Add Cruzamento</button>

        <button onClick={toggleSimulation} 
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

          nodesConnectable={!isSimulating} 
          elementsSelectable={!isSimulating}
          deleteKeyCode={isSimulating ? null : ['Backspace', ['Delete']]}
          fitView
        >
          <Background />
          <Controls />
        </ReactFlow>
      </ReactFlowProvider>
    </div>
  );
}