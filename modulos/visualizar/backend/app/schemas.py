from pydantic import BaseModel
from typing import Dict, Any

class ConfiguracaoSimulacao(BaseModel):
    tipo_evento: str = "INICIAR_SIMULACAO"
    qtd_veiculos: int
    dados_grafo: Dict[str, Any]