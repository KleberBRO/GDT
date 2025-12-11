import asyncio
import os
import threading
import json
import random
from datetime import datetime
from collections import deque, defaultdict
from kafka import KafkaProducer, KafkaConsumer

class VeiculoSimulador:
    def __init__(self, veiculo_id, rota_nodos):
        self.veiculo_id = veiculo_id
        self.rota_nodos = rota_nodos  # Lista de IDs de nodos: ['1', '2', '5']
        self.passo_atual = 0
        self.concluido = False
        self.criado_em = datetime.now()

    def obter_proxima_via(self):
        """Retorna a aresta atual (Ex: '1-2') e avança o passo."""
        if self.passo_atual < len(self.rota_nodos) - 1:
            origem = self.rota_nodos[self.passo_atual]
            destino = self.rota_nodos[self.passo_atual + 1]
            via = f"{origem}-{destino}"
            
            self.passo_atual += 1
            if self.passo_atual >= len(self.rota_nodos) - 1:
                self.concluido = True
            
            return via
        return None

class Simulador:
    def __init__(self, kafka_broker="localhost:9092"):
        self.kafka_broker = kafka_broker
        self.rodando = False
        self.contador_veiculos = 0
        self.veiculos_ativos = [] # Lista de objetos VeiculoSimulador
        
        # Estruturas do Grafo
        self.grafo = defaultdict(list) # Lista de adjacências: {'1': ['2'], '2': ['3', '4']}
        self.nodos = []

        # Configurações Kafka
        self.producer = KafkaProducer(
            bootstrap_servers=[self.kafka_broker],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        self.consumer = KafkaConsumer(
            "sistema.configuracao",
            bootstrap_servers=[self.kafka_broker],
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='simulador-group',
            auto_offset_reset='latest'
        )

    def construir_grafo(self, dados_grafo):
        """Reconstroi o grafo a partir do JSON recebido."""
        self.grafo.clear()
        self.nodos = dados_grafo.get('nodos', [])
        arestas = dados_grafo.get('arestas', [])
        
        print(f"Construindo grafo com {len(self.nodos)} nodos e {len(arestas)} arestas.")
        
        for aresta in arestas:
            # Esperado formato "origem-destino" (ex: "1-2")
            if '-' in aresta:
                origem, destino = aresta.split('-')
                self.grafo[origem].append(destino)

    def calcular_rota_bfs(self, inicio, fim):
        """Encontra o menor caminho entre inicio e fim usando BFS."""
        if inicio == fim:
            return [inicio]
        
        fila = deque([[inicio]])
        visitados = {inicio}
        
        while fila:
            caminho = fila.popleft()
            nodo_atual = caminho[-1]
            
            if nodo_atual == fim:
                return caminho
            
            for vizinho in self.grafo.get(nodo_atual, []):
                if vizinho not in visitados:
                    visitados.add(vizinho)
                    nova_rota = list(caminho)
                    nova_rota.append(vizinho)
                    fila.append(nova_rota)
                    
        return None # Não há caminho

    def gerar_veiculo(self):
        if not self.nodos or not self.grafo:
            print("Aviso: Grafo vazio ou não configurado. Impossível gerar veículos.")
            return

        # Tenta encontrar uma rota válida (tenta 5 vezes antes de desistir do ciclo)
        for _ in range(5):
            origem = random.choice(self.nodos)
            destino = random.choice(self.nodos)
            
            if origem == destino:
                continue

            rota = self.calcular_rota_bfs(origem, destino)
            
            if rota and len(rota) > 1:
                self.contador_veiculos += 1
                novo_veiculo = VeiculoSimulador(self.contador_veiculos, rota)
                self.veiculos_ativos.append(novo_veiculo)
                print(f"Veículo {novo_veiculo.veiculo_id} criado. Rota: {rota}")
                return

    def processar_movimentos(self):
        """Itera sobre os veículos ativos e move eles para a próxima via."""
        for veiculo in self.veiculos_ativos[:]: # Copia da lista para poder remover itens
            via_atual = veiculo.obter_proxima_via()
            
            if via_atual:
                # Envia evento sensor.veiculo
                payload = {
                    "id_veiculo": f"carro_{veiculo.veiculo_id}",
                    "id_via": via_atual,
                    "timestamp": int(datetime.now().timestamp())
                }
                try:
                    self.producer.send("sensor.veiculo", payload)
                    print(f"-> Carro {veiculo.veiculo_id} entrou na via {via_atual}")
                except Exception as e:
                    print(f"Erro Kafka: {e}")
            
            if veiculo.concluido:
                print(f"Veículo {veiculo.veiculo_id} chegou ao destino.")
                self.veiculos_ativos.remove(veiculo)

    def escutar_comandos(self):
        print("Simulador: Ouvindo sistema.configuracao...")
        for mensagem in self.consumer:
            try:
                dados = mensagem.value
                tipo = dados.get("tipo_evento")
                
                if tipo == "INICIAR_SIMULACAO":
                    print(">>> INICIAR SIMULAÇÃO RECEBIDO")
                    self.construir_grafo(dados.get("dados_grafo", {}))
                    # Opcional: ajustar quantidade de veículos ou frequência baseado no payload
                    self.rodando = True
                
                elif tipo == "PARAR_SIMULACAO":
                    print(">>> PARAR SIMULAÇÃO RECEBIDO")
                    self.rodando = False
                    self.veiculos_ativos.clear() # Limpa os carros ao parar
                        
            except Exception as e:
                print(f"Erro ao processar comando: {e}")

    async def executar_simulacao(self):
        # Thread para ouvir Kafka sem bloquear
        threading.Thread(target=self.escutar_comandos, daemon=True).start()
        print(f"Simulador iniciado (Broker: {self.kafka_broker})")

        ultimo_spawn = datetime.now()
        spawn_rate = 2.0 # Segundos entre criação de novos carros

        while True:
            if self.rodando:
                agora = datetime.now()
                
                # 1. Gerar novos carros periodicamente
                if (agora - ultimo_spawn).total_seconds() > spawn_rate:
                    self.gerar_veiculo()
                    ultimo_spawn = agora

                # 2. Mover carros existentes
                self.processar_movimentos()

            # Loop de "tick" da simulação (move os carros a cada 1 segundo)
            await asyncio.sleep(1)

if __name__ == "__main__":
    kafka_broker = os.getenv('KAFKA_BROKER', "localhost:9092")
    simulador = Simulador(kafka_broker=kafka_broker)
    
    try:
        asyncio.run(simulador.executar_simulacao())
    except KeyboardInterrupt:
        print("Encerrando...")