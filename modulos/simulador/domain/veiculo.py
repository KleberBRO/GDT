from datetime import datetime

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