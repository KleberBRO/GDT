from collections import deque, defaultdict
import random

class GerenciadorGrafo:
    def __init__(self):
        self.grafo = defaultdict(list)
        self.nodos = []
        self.direcao_vias = {}

    def limpar(self):
        self.grafo.clear()
        self.direcao_vias.clear()
        self.nodos = []

    def construir(self, dados_grafo):
        self.limpar()
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

    def gerar_rota_aleatoria(self):
        if not self.nodos or not self.grafo:
            return None
        
        # Tenta achar uma rota válida algumas vezes
        for _ in range(5):
            origem = random.choice(self.nodos)
            destino = random.choice(self.nodos)
            if origem == destino: continue
            
            rota = self.calcular_rota_bfs(origem, destino)
            if rota and len(rota) > 1:
                return rota
        return None

    def obter_direcao_via(self, via):
        return self.direcao_vias.get(via)