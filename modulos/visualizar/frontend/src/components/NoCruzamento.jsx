import React, { memo } from 'react';
import { Handle, Position } from 'reactflow';

// Este componente recebe dados via 'data' (vindo do estado do React Flow)
const noCruzamento = ({ data }) => {
  // data.status virá do Kafka -> FastAPI -> Frontend
  const isGreen = data.status === 'VERDE'; 
  
  return (
    <div style={{ 
      padding: '10px', 
      border: '1px solid #777', 
      borderRadius: '5px', 
      background: '#fff',
      minWidth: '150px'
    }}>
      {/* Handles são os pontos de conexão para as vias (Edges) */}
      <Handle type="target" position={Position.Top} />
      
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <strong>Cruzamento {data.id}</strong>
        
        {/* Visualização do Semáforo */}
        <div style={{ 
          marginTop: '10px',
          width: '20px', 
          height: '20px', 
          borderRadius: '50%', 
          backgroundColor: isGreen ? '#4CAF50' : '#F44336',
          boxShadow: `0 0 10px ${isGreen ? '#4CAF50' : '#F44336'}`
        }} />
        
        <span style={{ fontSize: '12px', marginTop: '5px' }}>
          Fila: {data.queueCount || 0} carros
        </span>
        
        {/* Indicador de alerta de espera longa [cite: 8] */}
        {data.longWaitAlert && <span style={{color: 'red', fontWeight: 'bold'}}>⚠ Atraso!</span>}
      </div>

      <Handle type="source" position={Position.Bottom} />
    </div>
  );
};

export default memo(noCruzamento);