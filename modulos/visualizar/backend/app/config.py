import os
import logging

# Configuração de Logs
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("backend")

# Configurações do Kafka
KAFKA_BROKER = os.getenv("KAFKA_BROKER", "kafka:9092")

TOPICS_TO_CONSUME = [
    "veiculos",
    "sensor.veiculo",
    "cruzamento.status",
    "orquestrador.comando"
]

TOPIC_CONFIG = "sistema.configuracao"