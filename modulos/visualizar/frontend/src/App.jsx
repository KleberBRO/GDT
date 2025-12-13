import React, { useState, useCallback, useEffect, useRef } from 'react';
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

  // Mapeia ID_VEICULO -> ID_VIA_ATUAL (ex: "carro_1" -> "1-2")
  const vehiclePositions = useRef({});

  useEffect(() => {
    const ws = new WebSocket('ws://localhost:8000/ws');
    ws.onopen = () => console.log('WebSocket conectado');
    ws.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        handleSocketMessage(message);
      } catch (error) {
        console.error('Erro WebSocket:', error);
      }
    };
    return () => ws.close();
  }, []);

  const handleSocketMessage = (msg) => {
    const { topic, data } = msg;

    if (topic === 'cruzamento.status') {
      setNodes((nds) =>
        nds.map((node) => {
          if (node.id === data.id_cruzamento) {
            let visualStatus = 'VERMELHO';
            if (data.status_sinal === 'N-S') visualStatus = 'VERDE';
            else if (data.status_sinal === 'L-O') visualStatus = 'VERDE_H';
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

    if (topic === 'sensor.veiculo') {
      const { id_veiculo, id_via } = data;
      const previousVia = vehiclePositions.current[id_veiculo];

      // Atualiza o registro de onde o carro está
      if (id_via === 'FIM') {
        delete vehiclePositions.current[id_veiculo];
      } else {
        vehiclePositions.current[id_veiculo] = id_via;
      }

      setEdges((eds) => eds.map((edge) => {
        // Reconstrói o ID da via baseado na aresta (origem-destino)
        const edgeViaId = `${edge.source}-${edge.target}`;
        
        let newCount = edge.data.queueCount || 0;

        // 1. Decrementa da via anterior (se houver)
        if (previousVia && edgeViaId === previousVia) {
           newCount = Math.max(0, newCount - 1);
        }

        // 2. Incrementa na via atual (se não for FIM)
        if (id_via !== 'FIM' && edgeViaId === id_via) {
           newCount += 1;
        }

        // Só atualiza se mudou algo (opcional, mas bom para performance)
        if (newCount !== edge.data.queueCount) {
            return {
                ...edge,
                data: { ...edge.data, queueCount: newCount }
            };
        }
        return edge;
      }));
    }
  };

  const onNodesChange = useCallback((changes) => setNodes((nds) => applyNodeChanges(changes, nds)), []);
  const onEdgesChange = useCallback((changes) => setEdges((eds) => applyEdgeChanges(changes, eds)), []);
  
  const onConnect = useCallback((params) => {
        if (isSimulating) return; 
        setEdges((eds) => addEdge({ ...params, type: 'custom', animated: true, data: { queueCount: 0 } }, eds));
  }, [isSimulating]);

  const onNodesDelete = useCallback((deleted) => {
      if (isSimulating) return;
      setEdges((eds) => {
        const connectedEdges = getConnectedEdges(deleted, eds);
        return eds.filter((e) => !connectedEdges.some((ce) => ce.id === e.id));
      });
    }, [isSimulating]);

  const onEdgeDoubleClick = useCallback((event, edge) => {
    if (isSimulating) return; 
    setEdges((eds) => eds.filter((e) => e.id !== edge.id));
  }, [isSimulating]);

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
    const eventType = isSimulating ? 'PARAR_SIMULACAO' : 'INICIAR_SIMULACAO';
    
    if (!isSimulating) {
        // Limpa contadores visuais e memória local de veículos ao iniciar
        setEdges((eds) => eds.map(e => ({...e, data: { ...e.data, queueCount: 0 }})));
        vehiclePositions.current = {};
    }

    const arestasFormatadas = edges.map(e => {
        const sourceNode = nodes.find(n => n.id === e.source);
        const targetNode = nodes.find(n => n.id === e.target);
        let dir = 'N'; 
        if (sourceNode && targetNode) {
             const dx = targetNode.position.x - sourceNode.position.x;
             const dy = targetNode.position.y - sourceNode.position.y;
             if (Math.abs(dx) > Math.abs(dy)) dir = dx > 0 ? 'O' : 'L'; 
             else dir = dy > 0 ? 'N' : 'S';
        }
        return `${e.source}-${e.target}-${dir}`;
    });

    const payload = {
      tipo_evento: eventType,
      qtd_veiculos: isSimulating ? 0 : Number(qtdVeiculos),
      dados_grafo: {
        nodos: nodes.map(n => n.id),
        arestas: arestasFormatadas
      }
    };

    try {
      await fetch('http://localhost:8000/configurar-simulacao', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      setIsSimulating(!isSimulating);
    } catch (error) {
      console.error('Erro ao comunicar com simulação:', error);
      alert('Falha ao comunicar com o servidor.');
    }
  };

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      <div className='controls-container'>
        <h3>Controle da Simulação</h3>
        <div>
        <label>Qtd Veículos:</label>
        <input type="number" value={qtdVeiculos} onChange={(e) => setQtdVeiculos(e.target.value)} style={{ width: '60px', marginLeft: '5px' }} min="1" />
        </div>
        <button onClick={addIntersection} disabled={isSimulating} className="control-button btn-add">+ Add Cruzamento</button>
        <button onClick={toggleSimulation} className={`control-button ${isSimulating ? 'btn-stop' : ''}`}>
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
          deleteKeyCode={isSimulating ? null : ['Backspace', 'Delete']}
          fitView
        >
          <Background />
          <Controls />
        </ReactFlow>
      </ReactFlowProvider>
    </div>
  );
}