import { useState, useRef, useEffect, useCallback } from 'react';
import { applyNodeChanges, applyEdgeChanges, addEdge, getConnectedEdges } from 'reactflow';

const initialNodes = [
  { 
    id: '1', 
    type: 'trafficLight', 
    position: { x: 250, y: 5 }, 
    data: { id: '1', status: 'VERMELHO', queueCount: 5 } 
  },
];

export function useTrafficSimulation() {
  const [nodes, setNodes] = useState(initialNodes);
  const [edges, setEdges] = useState([]);
  const [qtdVeiculos, setQtdVeiculos] = useState(50);
  const [isSimulating, setIsSimulating] = useState(false);
  const vehiclePositions = useRef({});

  // WebSocket Logic
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

      if (id_via === 'FIM') {
        delete vehiclePositions.current[id_veiculo];
      } else {
        vehiclePositions.current[id_veiculo] = id_via;
      }

      setEdges((eds) => eds.map((edge) => {
        const edgeViaId = `${edge.source}-${edge.target}`;
        let newCount = edge.data.queueCount || 0;

        if (previousVia && edgeViaId === previousVia) newCount = Math.max(0, newCount - 1);
        if (id_via !== 'FIM' && edgeViaId === id_via) newCount += 1;

        if (newCount !== edge.data.queueCount) {
            return { ...edge, data: { ...edge.data, queueCount: newCount } };
        }
        return edge;
      }));
    }
  };

  // ReactFlow Handlers
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

  const exportGraph = useCallback(() => {
    // Cria um objeto com os dados atuais
    const graphData = { nodes, edges };
    
    // Converte para string JSON
    const jsonString = JSON.stringify(graphData, null, 2);
    
    // Cria um Blob e um link temporário para download
    const blob = new Blob([jsonString], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = "grafo_transito.json";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [nodes, edges]);

const importGraph = useCallback((event) => {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const json = JSON.parse(e.target.result);
        
        if (json.nodes && json.edges) {
          if (isSimulating) {
            alert("Pare a simulação antes de carregar um novo grafo.");
            return;
          }
          setNodes(json.nodes);
          setEdges(json.edges);
        } else {
          alert("Arquivo inválido.");
        }
      } catch (error) {
        console.error("Erro ao ler o arquivo JSON:", error);
        alert("Erro ao processar o arquivo.");
      }
    };
    reader.readAsText(file);
    event.target.value = ''; 
    
  }, [isSimulating]);

  const toggleSimulation = async () => {
    const eventType = isSimulating ? 'PARAR_SIMULACAO' : 'INICIAR_SIMULACAO';
    
    if (!isSimulating) {
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

  return {
    nodes, edges,
    onNodesChange, onEdgesChange,
    onConnect, onNodesDelete, onEdgeDoubleClick,
    addIntersection, toggleSimulation,
    qtdVeiculos, setQtdVeiculos, isSimulating,
    exportGraph, importGraph
  };
}