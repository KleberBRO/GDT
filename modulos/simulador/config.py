import os

KAFKA_BROKER = os.getenv('KAFKA_BROKER', "localhost:9092")
TOPIC_CONFIG = "sistema.configuracao"
TOPIC_STATUS = "cruzamento.status"
TOPIC_VEICULO = "sensor.veiculo"