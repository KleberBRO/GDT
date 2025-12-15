import asyncio
import json
import threading
from datetime import datetime
from kafka import KafkaProducer, KafkaConsumer
from domain.veiculo import VeiculoSimulador
from services.grafo import GerenciadorGrafo
import config

class MotorSimulacao:
    def __init__(self):
        self.rodando = False
        self.contador_veiculos = 0
        self.veiculos_ativos = [] 
        self.max_veiculos = 50
        self.estado_cruzamentos = {}
        
        # Serviço de Grafo
        self.grafo_manager = GerenciadorGrafo()

        # Kafka
        self.producer = KafkaProducer(
            bootstrap_servers=[config.KAFKA_BROKER],
            value_serializer=lambda v: json.dumps(v).encode('utf-8')
        )
        self.consumer = KafkaConsumer(
            config.TOPIC_CONFIG,
            config.TOPIC_STATUS,
            bootstrap_servers=[config.KAFKA_BROKER],
            value_deserializer=lambda m: json.loads(m.decode('utf-8')),
            group_id='simulador-group',
            auto_offset_reset='latest'
        )

    def gerar_veiculo(self):
        rota = self.grafo_manager.gerar_rota_aleatoria()
        if rota:
            self.contador_veiculos += 1
            novo_veiculo = VeiculoSimulador(self.contador_veiculos, rota)
            self.veiculos_ativos.append(novo_veiculo)

    def pode_passar(self, veiculo):
        if veiculo.passo_atual == 0: return True
        
        nodo_anterior = veiculo.rota_nodos[veiculo.passo_atual - 1]
        nodo_cruzamento = veiculo.rota_nodos[veiculo.passo_atual]
        via_chegada = f"{nodo_anterior}-{nodo_cruzamento}"
        
        direcao_chegada = self.grafo_manager.obter_direcao_via(via_chegada)
        status = self.estado_cruzamentos.get(nodo_cruzamento, 'L-O')
        
        if not direcao_chegada: return True
        
        if status == 'N-S': return direcao_chegada in ['N', 'S']
        elif status == 'L-O': return direcao_chegada in ['L', 'O']
        return False

    def enviar_evento_veiculo(self, veiculo, id_via):
        try:
            self.producer.send(config.TOPIC_VEICULO, {
                "idVeiculo": f"carro_{veiculo.veiculo_id}",
                "idVia": id_via,
                "timestamp": int(datetime.now().timestamp() * 1000)  # milliseconds
            })
        except Exception as e:
            print(f"Erro Kafka: {e}")

    def processar_movimentos(self):
        for veiculo in self.veiculos_ativos[:]:
            # Verifica se chegou ao destino
            if veiculo.passo_atual == len(veiculo.rota_nodos) - 1:
                if self.pode_passar(veiculo):
                    self.enviar_evento_veiculo(veiculo, "FIM")
                    veiculo.concluido = True
                    self.veiculos_ativos.remove(veiculo)
                continue

            if not self.pode_passar(veiculo):
                continue

            via_atual = veiculo.obter_proxima_via()
            if via_atual:
                self.enviar_evento_veiculo(veiculo, via_atual)

    def escutar_comandos(self):
        print("Simulador: Ouvindo tópicos Kafka...")
        for mensagem in self.consumer:
            try:
                dados = mensagem.value
                topic = mensagem.topic
                
                if topic == config.TOPIC_CONFIG:
                    tipo = dados.get("tipo_evento")
                    if tipo == "INICIAR_SIMULACAO":
                        print(">>> INICIAR SIMULAÇÃO")
                        self.grafo_manager.construir(dados.get("dados_grafo", {}))
                        self.max_veiculos = int(dados.get("qtd_veiculos", 50))
                        self.rodando = True
                    elif tipo == "PARAR_SIMULACAO":
                        print(">>> PARAR SIMULAÇÃO")
                        self.rodando = False
                        self.veiculos_ativos.clear()
                        self.estado_cruzamentos.clear()
                
                elif topic == config.TOPIC_STATUS:
                    self.estado_cruzamentos[dados.get("id_cruzamento")] = dados.get("status_sinal")
            
            except Exception as e:
                print(f"Erro ao processar mensagem: {e}")

    async def executar(self):
        threading.Thread(target=self.escutar_comandos, daemon=True).start()
        print(f"Simulador iniciado (Broker: {config.KAFKA_BROKER})")
        
        contador_ciclos = 0
        while True:
            if self.rodando:
                contador_ciclos += 1
                if contador_ciclos % 10 == 0:  # Log a cada 10 ciclos (10 segundos)
                    print(f"[CICLO {contador_ciclos}] Rodando={self.rodando}, Veículos ativos={len(self.veiculos_ativos)}/{self.max_veiculos}, Nodos={len(self.grafo_manager.nodos)}")
                
                # Gera novos veículos se necessário
                while len(self.veiculos_ativos) < self.max_veiculos:
                    if not self.grafo_manager.nodos: 
                        print(f"[ERRO] Grafo sem nodos! Nodos: {self.grafo_manager.nodos}")
                        break
                    self.gerar_veiculo()
                    print(f"[VEICULO] Novo veículo gerado. Total: {len(self.veiculos_ativos)}")
                
                self.processar_movimentos()
            
            await asyncio.sleep(1)