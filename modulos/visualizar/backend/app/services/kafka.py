import json
import time
import asyncio
import threading
from kafka import KafkaProducer, KafkaConsumer
from kafka.errors import NoBrokersAvailable

from app.config import logger, KAFKA_BROKER, TOPICS_TO_CONSUME
from app.services.websocket import manager

# Variável global para o producer
producer = None

def get_producer():
    """Retorna a instância atual do producer."""
    return producer

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

async def init_producer():
    """Inicializa o Producer com lógica de retry."""
    global producer
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

def close_producer():
    if producer:
        producer.close()