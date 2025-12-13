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
        self.rota_nodos = rota_nodos
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
            return via
        return None

class Simulador:
    def __init__(self, kafka_broker="localhost:9092"):
        self.kafka_broker = kafka_broker
        self.rodando = False
        self.contador_veiculos = 0
        self.veiculos_ativos = [] 
        self.max_veiculos = 50
        
        self.grafo = defaultdict(list)
        self.nodos = []
        self.direcao_vias = {} 
        self.estado_cruzamentos = {}

        self.producer = KafkaProducer(
            bootstrap_servers=[self.kafka_broker],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        self.consumer = KafkaConsumer(
            "sistema.configuracao",
            "cruzamento.status",
            bootstrap_servers=[self.kafka_broker],
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='simulador-group',
            auto_offset_reset='latest'
        )

    def construir_grafo(self, dados_grafo):
        self.grafo.clear()
        self.direcao_vias.clear()
        self.nodos = dados_grafo.get('nodos', [])
        arestas = dados_grafo.get('arestas', [])
        print(f"Construindo grafo com {len(self.nodos)} nodos e {len(arestas)} arestas.")
        for aresta in arestas:
            partes = aresta.split('-')
            if len(partes) >= 2:
                origem = partes[0]
                destino = partes[1]
                self.grafo[origem].append(destino)
                if len(partes) == 3:
                    direcao = partes[2]
                    self.direcao_vias[f"{origem}-{destino}"] = direcao

    def calcular_rota_bfs(self, inicio, fim):
        if inicio == fim: return [inicio]
        fila = deque([[inicio]])
        visitados = {inicio}
        while fila:
            caminho = fila.popleft()
            nodo_atual = caminho[-1]
            if nodo_atual == fim: return caminho
            for vizinho in self.grafo.get(nodo_atual, []):
                if vizinho not in visitados:
                    visitados.add(vizinho)
                    nova_rota = list(caminho) + [vizinho]
                    fila.append(nova_rota)
        return None

    def gerar_veiculo(self):
        if not self.nodos or not self.grafo: return
        for _ in range(5):
            origem = random.choice(self.nodos)
            destino = random.choice(self.nodos)
            if origem == destino: continue
            rota = self.calcular_rota_bfs(origem, destino)
            if rota and len(rota) > 1:
                self.contador_veiculos += 1
                novo_veiculo = VeiculoSimulador(self.contador_veiculos, rota)
                self.veiculos_ativos.append(novo_veiculo)
                print(f"Veículo {novo_veiculo.veiculo_id} criado. Rota: {rota}")
                return

    def pode_passar(self, veiculo):
        if veiculo.passo_atual == 0: return True
        nodo_anterior = veiculo.rota_nodos[veiculo.passo_atual - 1]
        nodo_cruzamento = veiculo.rota_nodos[veiculo.passo_atual]
        via_chegada = f"{nodo_anterior}-{nodo_cruzamento}"
        direcao_chegada = self.direcao_vias.get(via_chegada)
        status = self.estado_cruzamentos.get(nodo_cruzamento, 'L-O')
        
        if not direcao_chegada: return True
        if status == 'N-S': return direcao_chegada in ['N', 'S']
        elif status == 'L-O': return direcao_chegada in ['L', 'O']
        return False

    def processar_movimentos(self):
        for veiculo in self.veiculos_ativos[:]:
            # Chegou ao fim da rota
            if veiculo.passo_atual == len(veiculo.rota_nodos) - 1:
                if self.pode_passar(veiculo):
                    print(f"Veículo {veiculo.veiculo_id} chegou ao destino.")
                    
                    # --- NOVO: Avisar que saiu do sistema ---
                    try:
                        self.producer.send("sensor.veiculo", {
                            "id_veiculo": f"carro_{veiculo.veiculo_id}",
                            "id_via": "FIM",
                            "timestamp": int(datetime.now().timestamp())
                        })
                    except Exception as e:
                        print(f"Erro ao enviar finalização: {e}")
                    # ----------------------------------------

                    veiculo.concluido = True
                    self.veiculos_ativos.remove(veiculo)
                continue

            if not self.pode_passar(veiculo):
                continue

            via_atual = veiculo.obter_proxima_via()
            if via_atual:
                try:
                    self.producer.send("sensor.veiculo", {
                        "id_veiculo": f"carro_{veiculo.veiculo_id}",
                        "id_via": via_atual,
                        "timestamp": int(datetime.now().timestamp())
                    })
                    print(f"-> Carro {veiculo.veiculo_id} entrou na via {via_atual}")
                except Exception as e:
                    print(f"Erro Kafka: {e}")

    def escutar_comandos(self):
        print("Simulador: Ouvindo sistema.configuracao e cruzamento.status...")
        for mensagem in self.consumer:
            try:
                dados = mensagem.value
                topic = mensagem.topic
                if topic == "sistema.configuracao":
                    tipo = dados.get("tipo_evento")
                    if tipo == "INICIAR_SIMULACAO":
                        print(">>> INICIAR SIMULAÇÃO")
                        self.construir_grafo(dados.get("dados_grafo", {}))
                        self.max_veiculos = int(dados.get("qtd_veiculos", 50))
                        self.rodando = True
                    elif tipo == "PARAR_SIMULACAO":
                        print(">>> PARAR SIMULAÇÃO")
                        self.rodando = False
                        self.veiculos_ativos.clear()
                        self.estado_cruzamentos.clear()
                elif topic == "cruzamento.status":
                    self.estado_cruzamentos[dados.get("id_cruzamento")] = dados.get("status_sinal")
            except Exception as e:
                print(f"Erro: {e}")

    async def executar_simulacao(self):
        threading.Thread(target=self.escutar_comandos, daemon=True).start()
        print(f"Simulador iniciado (Broker: {self.kafka_broker})")
        while True:
            if self.rodando:
                while len(self.veiculos_ativos) < self.max_veiculos:
                    if not self.nodos: break
                    self.gerar_veiculo()
                self.processar_movimentos()
            await asyncio.sleep(1)

if __name__ == "__main__":
    kafka_broker = os.getenv('KAFKA_BROKER', "localhost:9092")
    simulador = Simulador(kafka_broker=kafka_broker)
    try:
        asyncio.run(simulador.executar_simulacao())
    except KeyboardInterrupt:
        print("Encerrando...")