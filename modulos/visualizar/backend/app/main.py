import asyncio
import threading
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import logger
from app.services.kafka import kafka_consumer_thread, init_producer, close_producer
from app.routers import api, websocket

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. Recupera o loop de eventos atual
    loop = asyncio.get_running_loop()
    
    # 2. Inicia a Thread do Consumer
    t = threading.Thread(target=kafka_consumer_thread, args=(loop,), daemon=True)
    t.start()

    # 3. Inicia o Producer (Async)
    await init_producer()

    yield
    
    # 4. Cleanup
    close_producer()
    logger.info("Backend finalizado.")

# Criação do App
app = FastAPI(lifespan=lifespan)

# Configuração CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Inclusão das Rotas
app.include_router(api.router)
app.include_router(websocket.router)