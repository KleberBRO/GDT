// modulos/visualizar/frontend/src/components/CustomEdge.jsx
import React from 'react';
import { BaseEdge, EdgeLabelRenderer, getBezierPath } from 'reactflow';

export default function CustomEdge({
  id,
  sourceX,
  sourceY,
  targetX,
  targetY,
  sourcePosition,
  targetPosition,
  style = {},
  markerEnd,
  data, // Aqui receberemos os dados da fila (data.queueCount)
}) {
  // Calcula o caminho da linha (Bezier é a curva padrão)
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });

  const queueCount = data?.queueCount || 0;
  const maxVeiculos = data?.maxVeiculos || 50;
  const limiteParaVermelho = Math.max(1, maxVeiculos * 0.2); 
  const ratio = Math.min(queueCount / limiteParaVermelho, 1);
  // Interpola entre Preto (0,0,0) e Vermelho (255,0,0)
  const redComponent = Math.floor(255 * ratio);
  // Cria a string de cor
  const edgeColor = `rgb(${redComponent}, 0, 0)`;
  
  // Aumenta a espessura se tiver carros para facilitar a visualização
  const edgeWidth = ratio > 0 ? 3 : 2;

  return (
    <>
      <BaseEdge 
        path={edgePath} 
        markerEnd={markerEnd} 
        style={{ 
            ...style, 
            stroke: edgeColor, // Aplica a cor calculada
            strokeWidth: edgeWidth,
            transition: 'stroke 0.5s ease' // Suaviza a troca de cor
        }} 
      />
      
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            background: queueCount > 0 ? edgeColor : '#fff', // Fundo muda com a cor
            color: queueCount > 0 ? '#fff' : '#000', // Texto inverte para contraste
            padding: '4px 6px',
            borderRadius: '5px',
            fontSize: 12,
            fontWeight: 700,
            pointerEvents: 'all',
          }}
          className="nodrag nopan"
        >
          {queueCount}
        </div>
      </EdgeLabelRenderer>
    </>
  );
}