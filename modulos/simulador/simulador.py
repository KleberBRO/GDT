import asyncio
from kafka import KafkaProducer
from kafka.errors import KafkaError
import json
import random
from datetime import datetime

class VeiculoSimulador:
    def _init_(self, veiculo_id, origem, destino):
        self.veiculo_id = veiculo_id
        self.origem = origem
        self.destino = destino
        self.posicao_atual = origem
        self.criado_em = datetime.now()
    
    def para_dict(self):
        return {
            "veiculo_id": self.veiculo_id,
            "origem": self.origem,
            "destino": self.destino,
            "posicao_atual": self.posicao_atual,
            "criado_em": self.criado_em.isoformat()
        }

class Simulador:
    def _init_(self, kafka_broker="localhost:9092"):
        self.kafka_broker = kafka_broker
        self.producer = KafkaProducer(
            bootstrap_servers=[self.kafka_broker],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        self.veiculos = []
        self.contador_veiculos = 0

        def gerar_veiculo(self):
            self.contador_veiculos += 1
            origem = random.randint(1,10)
            destino = random.randint(1,10)

            while destino == origem:
                destino = random.randint(1,10)

            veiculo = VeiculoSimulador(
                veiculo_id=self.contador_veiculos,
                origem=origem,
                destino=destino
            )
            self.veiculos.append(veiculo)

            # publica no kafka
            self.producer.send("veiculos", veiculo.para_dict())
            print(f"Veículo {veiculo.veiculo_id}: origem {veiculo.origem} -> destino {veiculo.destino}")

            return veiculo
        
    async def executar_simulacao(self, duracao_segundos=60, intervalo=5):
        inicio = datetime.now()

        while (datetime.now() - inicio).total_seconds() < duracao_segundos:
            self.gerar_veiculo()
            await asyncio.sleep(intervalo)

        self.producer.close()

if __name__ == "__main__":
    simulador = Simulador(kafka_broker="localhost:9092")
    asyncio.run(simulador.executar_simulacao(duracao_segundos=60, intervalo=5))
    
