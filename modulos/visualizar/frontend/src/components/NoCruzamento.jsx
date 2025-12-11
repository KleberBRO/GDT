import React, { memo } from 'react';
import { Handle, Position } from 'reactflow';

const getLightStyle = (isGreen, sidePosition) => ({
  position: 'absolute',
  width: '12px',
  height: '12px',
  borderRadius: '50%',
  backgroundColor: isGreen ? '#4CAF50' : '#F44336', // Verde ou Vermelho
  boxShadow: `0 0 6px ${isGreen ? '#4CAF50' : '#F44336'}`,
  border: '1px solid #fff',
  zIndex: 10, // Ficar acima de outros elementos
  ...sidePosition // Aplica o top/left/right/bottom específico
});

// Este componente recebe dados via 'data' (vindo do estado do React Flow)
const noCruzamento = ({ data }) => {
  // data.status virá do Kafka -> FastAPI -> Frontend
  const isGreen = data.status === 'VERDE'; 
  const isVerticalGreen = data.status === 'VERDE';
  const isHorizontalGreen = data.status === 'VERDE_H'; // Exemplo para futuro estado
  
  return (
    <div style={{ 
      padding: '10px', 
      border: '1px solid #777', 
      borderRadius: '8px', 
      background: '#fff',
      minWidth: '80px',
      minHeight: '80px',
      position: 'relative', // Importante para o absolute das luzes funcionar
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      textAlign: 'center'
    }}>

      {/* --- LUZES INDICADORAS (BOLINHAS) --- */}
      
      {/* Topo (Vertical) */}
      <div style={getLightStyle(isVerticalGreen, { top: '5px', left: '50%', transform: 'translateX(-50%)' })} title="Norte" />
      
      {/* Baixo (Vertical) */}
      <div style={getLightStyle(isVerticalGreen, { bottom: '5px', left: '50%', transform: 'translateX(-50%)' })} title="Sul" />

      {/* Esquerda (Horizontal - Inverso do Vertical neste exemplo) */}
      <div style={getLightStyle(isHorizontalGreen || !isVerticalGreen, { left: '5px', top: '50%', transform: 'translateY(-50%)' })} title="Oeste" />

      {/* Direita (Horizontal - Inverso do Vertical neste exemplo) */}
      <div style={getLightStyle(isHorizontalGreen || !isVerticalGreen, { right: '5px', top: '50%', transform: 'translateY(-50%)' })} title="Leste" />

      {/* Handles são os pontos de conexão para as vias (Edges) */}
      <Handle 
          type="target" 
          position={Position.Top} 
          id="top-target" 
          style={{left:'30%', backgroundColor: 'blue'}}
      />
      <Handle type="source" 
        position={Position.Top} 
        id="top-source"
        style={{left:'70%'}}
      />


      <Handle type="target" 
        position={Position.Bottom} 
        id="bottom-target" 
        style={{left:'70%', backgroundColor: 'blue'}}
      />
      <Handle type="source" 
        position={Position.Bottom} 
        id="bottom-source" 
        style={{left:'30%'}}
      />

      <Handle type="target"
        position={Position.Left}
        id="left-target" 
        style={{top:'30%', backgroundColor: 'blue'}}
       />

      <Handle type="source"
        position={Position.Left} 
        id="left-source" 
        style={{top:'70%'}}
      />

      <Handle type="target" 
        position={Position.Right} 
        id="right-target" 
        style={{top:'70%', backgroundColor: 'blue'}}
      />

      <Handle type="source" 
        position={Position.Right} 
        id="right-source" 
        style={{top:'30%'}}
      />

      <div>
        ID: {data.id}
        
        {/* Aviso de Atraso */}
        <div style={{marginTop: 5}}>
             {data.longWaitAlert && <span style={{color: 'red', fontWeight: 'bold', fontSize: '10px'}}>⚠ Atraso!</span>}
        </div>
      </div>

    </div>
  );
};

export default memo(noCruzamento);