from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from app.services.websocket import manager

router = APIRouter()

@router.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            # Mantém a conexão aberta aguardando mensagens (mesmo que não processe input do cliente)
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)