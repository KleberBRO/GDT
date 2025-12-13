import React from 'react';
import ReactFlow, { Background, Controls, ReactFlowProvider } from 'reactflow';
import 'reactflow/dist/style.css';
import './App.css';

import SimulationControls from './components/SimulationControls';
import NoCruzamento from './components/NoCruzamento';
import CustomEdge from './components/CustomEdge';

import { useTrafficSimulation } from './hooks/useTrafficSimulation';

const nodeTypes = { trafficLight: NoCruzamento };
const edgeTypes = { custom: CustomEdge };

export default function App() {
  const {
    nodes, edges,
    onNodesChange, onEdgesChange,
    onConnect, onNodesDelete, onEdgeDoubleClick,
    addIntersection, toggleSimulation,
    qtdVeiculos, setQtdVeiculos, isSimulating
  } = useTrafficSimulation();

  return (
    <div style={{ width: '100vw', height: '100vh' }}>
      
      <SimulationControls 
        qtdVeiculos={qtdVeiculos}
        setQtdVeiculos={setQtdVeiculos}
        addIntersection={addIntersection}
        toggleSimulation={toggleSimulation}
        isSimulating={isSimulating}
      />

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