import asyncio
import json
import logging
import os
import threading
import time
from contextlib import asynccontextmanager
from typing import List

from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from kafka import KafkaProducer, KafkaConsumer
from pydantic import BaseModel
from kafka.errors import NoBrokersAvailable

# --- Configurações ---
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("backend")

KAFKA_BROKER = os.getenv("KAFKA_BROKER", "kafka:9092")

TOPICS_TO_CONSUME = [
    "veiculos",
    "sensor.veiculo",
    "cruzamento.status",
    "orquestrador.comando"
]

TOPIC_CONFIG = "sistema.configuracao"

# --- Gerenciador de WebSockets ---
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
        for connection in self.active_connections[:]:
            try:
                await connection.send_json(message)
            except Exception as e:
                logger.error(f"Erro no broadcast: {e}")
                self.disconnect(connection)

manager = ConnectionManager()
producer = None

# --- Kafka Consumer em Background ---
def kafka_consumer_thread(loop):
    """
    Roda em uma thread separada.
    Tenta conectar ao Kafka repetidamente até conseguir.
    """
    consumer = None
    logger.info("Iniciando Thread do Consumer...")

    # Loop de conexão resiliente
    while True:
        try:
            logger.info(f"Tentando conectar Consumer a {KAFKA_BROKER}...")
            consumer = KafkaConsumer(
                *TOPICS_TO_CONSUME,
                bootstrap_servers=KAFKA_BROKER,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                group_id='backend-visualizador-group',
                auto_offset_reset='latest'
            )
            logger.info("Consumer conectado com sucesso!")
            break  # Sai do loop de retry se conectar
        except Exception as e:
            logger.warning(f"Kafka indisponível ({e}). Tentando novamente em 5s...")
            time.sleep(5)

    # Loop de consumo de mensagens
    try:
        for message in consumer:
            payload = {
                "topic": message.topic,
                "data": message.value
            }
            # Envia para o WebSocket usando o loop principal
            if loop and loop.is_running():
                asyncio.run_coroutine_threadsafe(manager.broadcast(payload), loop)
    except Exception as e:
        logger.error(f"Erro no consumo de mensagens: {e}")
    finally:
        if consumer:
            consumer.close()

# --- Ciclo de Vida da Aplicação ---
@asynccontextmanager
async def lifespan(app: FastAPI):
    global producer
    loop = asyncio.get_running_loop()
    
    # 1. Iniciar Thread do Consumer (Passando o loop corretamente)
    t = threading.Thread(target=kafka_consumer_thread, args=(loop,), daemon=True)
    t.start()

    # 2. Iniciar Producer com Retry
    while producer is None:
        try:
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BROKER,
                value_serializer=lambda v: json.dumps(v).encode('utf-8')
            )
            logger.info("Producer conectado com sucesso.")
        except NoBrokersAvailable:
            logger.warning("Kafka Producer indisponível. Tentando novamente em 2s...")
            await asyncio.sleep(2)
        except Exception as e:
            logger.error(f"Erro ao conectar Producer: {e}")
            await asyncio.sleep(2)

    yield
    
    if producer:
        producer.close()
    logger.info("Backend finalizado.")

app = FastAPI(lifespan=lifespan)

# --- CORS ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Modelos ---
class ConfiguracaoSimulacao(BaseModel):
    tipo_evento: str = "INICIAR_SIMULACAO"
    qtd_veiculos: int
    dados_grafo: dict

# --- Rotas ---
@app.get("/")
def read_root():
    return {
        "service": "Gestor de Trânsito - Backend",
        "status": "Online",
        "kafka_connected": producer is not None
    }

@app.post("/configurar-simulacao")
async def configurar(config: ConfiguracaoSimulacao):
    if not producer:
        raise HTTPException(status_code=503, detail="Kafka ainda não está pronto.")
    
    try:
        producer.send(TOPIC_CONFIG, config.model_dump())
        logger.info(f"Configuração enviada para {TOPIC_CONFIG}")
        return {"status": "Configuração enviada com sucesso"}
    except Exception as e:
        logger.error(f"Erro ao enviar: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        manager.disconnect(websocket)