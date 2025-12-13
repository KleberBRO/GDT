from typing import List
from fastapi import WebSocket
from app.config import logger

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)
        logger.info("Novo cliente WebSocket conectado.")

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        # Itera sobre uma cópia da lista para evitar erros de modificação durante iteração
        for connection in self.active_connections[:]:
            try:
                await connection.send_json(message)
            except Exception as e:
                logger.error(f"Erro no broadcast: {e}")
                self.disconnect(connection)

# Instância Singleton para ser usada em toda a aplicação
manager = ConnectionManager()