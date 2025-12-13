from fastapi import APIRouter, HTTPException
from app.schemas import ConfiguracaoSimulacao
from app.services.kafka import get_producer
from app.config import logger, TOPIC_CONFIG

router = APIRouter()

@router.get("/")
def read_root():
    # Correção aqui: removido 'HZ'
    producer = get_producer()
    return {
        "service": "Gestor de Trânsito - Backend",
        "status": "Online",
        "kafka_connected": producer is not None
    }

@router.post("/configurar-simulacao")
async def configurar(config: ConfiguracaoSimulacao):
    producer = get_producer()
    if not producer:
        raise HTTPException(status_code=503, detail="Kafka ainda não está pronto.")
    
    try:
        producer.send(TOPIC_CONFIG, config.model_dump())
        logger.info(f"Configuração enviada para {TOPIC_CONFIG}")
        return {"status": "Configuração enviada com sucesso"}
    except Exception as e:
        logger.error(f"Erro ao enviar: {e}")
        raise HTTPException(status_code=500, detail=str(e))