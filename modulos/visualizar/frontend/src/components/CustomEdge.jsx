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

  return (
    <>
      {/* Desenha a linha da aresta */}
      <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} />
      
      {/* Renderiza o contador de carros em cima da aresta */}
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            // Estilos para parecer uma etiqueta de trânsito ou contador
            background: '#ffcc00',
            padding: '4px 8px',
            borderRadius: '5px',
            fontSize: 12,
            fontWeight: 700,
            pointerEvents: 'all',
          }}
          className="nodrag nopan"
        >
          {data?.queueCount || 0} 🚗
        </div>
      </EdgeLabelRenderer>
    </>
  );
}