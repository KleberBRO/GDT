# Detalhes Técnicos de Integração

## 🔌 Conexão Frontend ↔ Backend

### WebSocket (Real-time Updates)
**URL**: `ws://localhost:8000/ws`

**Fluxo**:
1. Frontend estabelece conexão WebSocket ao inicializar
2. Backend consome mensagens do Kafka em thread separada
3. Quando uma mensagem chega no Kafka, o Backend faz broadcast via WebSocket
4. Frontend recebe e atualiza a UI em tempo real

**Exemplo de Mensagem**:
```json
{
  "topic": "cruzamento.status",
  "data": {
    "id_cruzamento": "1",
    "status_sinal": "N-S",
    "tamanho_fila": 12
  }
}
```

### REST API

**Base URL**: `http://localhost:8000`

#### GET `/`
Verifica status do backend
```bash
curl http://localhost:8000/
# Resposta:
{
  "service": "Gestor de Trânsito - Backend",
  "status": "Online",
  "kafka_connected": true
}
```

#### POST `/configurar-simulacao`
Inicia a simulação com configuração do grafo
```bash
curl -X POST http://localhost:8000/configurar-simulacao \
  -H "Content-Type: application/json" \
  -d '{
    "id_simulacao": "sim001",
    "tipo_evento": "SIMULACAO_INICIADA",
    "dados_grafo": {
      "cruzamentos": [
        {"id": "1", "nome": "Cruzamento 1", "latitude": 0, "longitude": 0},
        {"id": "2", "nome": "Cruzamento 2", "latitude": 1, "longitude": 0}
      ],
      "arestas": [
        {"origem": "1", "destino": "2", "distancia": 100}
      ]
    }
  }'
```

## 🎯 Fluxo de Mensagens Kafka

### Tópico: `sensor.veiculo`
**Produzido por**: Simulador
**Consumido por**: Cruzamento, Backend
**Estrutura**:
```json
{
  "id_veiculo": "v001",
  "id_via": "1-2",
  "posicao": 45.5,
  "velocidade": 30.0
}
```

### Tópico: `cruzamento.alerta`
**Produzido por**: Cruzamento (quando fila > threshold)
**Consumido por**: Orquestrador, Backend
**Estrutura**:
```json
{
  "id_cruzamento": "1",
  "prioridade": "ALTA",
  "tamanho_fila": 15,
  "timestamp": 1234567890
}
```

### Tópico: `cruzamento.status`
**Produzido por**: Cruzamento (continuamente)
**Consumido por**: Backend
**Estrutura**:
```json
{
  "id_cruzamento": "1",
  "status_sinal": "N-S",
  "tamanho_fila": 10,
  "timestamp": 1234567890
}
```

### Tópico: `orquestrador.comando`
**Produzido por**: Orquestrador
**Consumido por**: Cruzamento
**Estrutura**:
```json
{
  "id_cruzamento_alvo": "1",
  "comando": "ABRIR",
  "id_transacao": "CMD_123456"
}
```

### Tópico: `sistema.configuracao`
**Produzido por**: Backend (via REST)
**Consumido por**: Orquestrador
**Estrutura**:
```json
{
  "tipo_evento": "SIMULACAO_INICIADA",
  "dados_grafo": {
    "cruzamentos": [...],
    "arestas": [...]
  }
}
```

## 🏗️ Arquitetura dos Serviços Java

### Cruzamento Service

**Componentes principais**:
- `CruzamentoApplication` - Classe principal
- `CruzamentoConsumer` - Consome `sensor.veiculo` e `orquestrador.comando`
- `CruzamentoService` - Lógica de negócio
- `CruzamentoProducer` - Envia `cruzamento.status` e `cruzamento.alerta`

**Fluxo**:
```
Consumer recebe SensorVeiculo
         ↓
CruzamentoService.processarSensor()
         ↓
Analisa fila (se > threshold → alerta)
         ↓
Producer envia CruzamentoStatus + CruzamentoAlerta
```

### Orquestrador Service

**Componentes principais**:
- `OrquestradorApplication` - Classe principal
- `OrquestradorConsumer` - Consome `cruzamento.alerta` e `sistema.configuracao`
- `OrquestradorService` - Lógica de orquestração
- `OrquestradorProducer` - Envia `orquestrador.comando`
- `DadosGrafo` - Armazena configuração do grafo

**Fluxo**:
```
Consumer recebe CruzamentoAlerta
         ↓
OrquestradorService.tratarAlerta()
         ↓
Consulta grafo para determinar próximo cruzamento
         ↓
Producer envia OrquestradorComando para Cruzamento
```

## 🔄 Ciclo Completo de uma Simulação

### Tempo 0s - Inicialização
```
Frontend (botão "Iniciar")
    ↓
POST /configurar-simulacao
    ↓
Backend produz SistemaConfig → Kafka
    ↓
Orquestrador consome SistemaConfig
Orquestrador armazena DadosGrafo
```

### Tempo 1-5s - Coleta de Dados
```
Simulador produz SensorVeiculo → Kafka
    ↓
Cruzamento consome SensorVeiculo
Cruzamento analisa:
  - Qual via o veículo entrou
  - Qual cruzamento atinge
  - Tamanho da fila
```

### Tempo 5-10s - Detecção de Congestão
```
Se fila > 10 veículos:
    ↓
Cruzamento produz CruzamentoAlerta → Kafka
    ↓
Orquestrador consome CruzamentoAlerta
Orquestrador consulta DadosGrafo:
  - Qual semáforo abrir
  - Qual via priorizar
```

### Tempo 10-15s - Orquestração
```
Orquestrador produz OrquestradorComando → Kafka
    ↓
Cruzamento consome OrquestradorComando
Cruzamento muda status do semáforo (VERMELHO → VERDE)
Cruzamento produz CruzamentoStatus → Kafka
```

### Tempo 15+ - Visualização
```
Backend consome CruzamentoStatus
    ↓
Backend broadcast via WebSocket
    ↓
Frontend recebe (< 100ms)
    ↓
UI atualiza em tempo real
```

## 🐛 Debugging

### Ver todas as mensagens Kafka
```bash
# Terminal 1: Iniciar console consumer
docker exec -it <kafka-container> \
  kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 \
    --topic cruzamento.status \
    --from-beginning

# Trocar "cruzamento.status" para outro tópico conforme necessário
```

### Monitorar WebSocket no Frontend
```javascript
// Abrir DevTools (F12) → Console
// Adicionar listeners:
const ws = new WebSocket('ws://localhost:8000/ws');
ws.addEventListener('message', (e) => {
  console.log('WebSocket message:', JSON.parse(e.data));
});
```

### Verificar Configuração Kafka no Container
```bash
docker exec -it <kafka-container> \
  kafka-topics.sh \
    --bootstrap-server localhost:9092 \
    --list
```

### Ver Configuração dos Consumers
```bash
docker exec -it <kafka-container> \
  kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --list

# Detalhes de um grupo:
docker exec -it <kafka-container> \
  kafka-consumer-groups.sh \
    --bootstrap-server localhost:9092 \
    --group cruzamento-group \
    --describe
```

## 🔒 Segurança

Atualmente o projeto não tem:
- ❌ Autenticação Kafka
- ❌ Encriptação SSL/TLS
- ❌ Validação de tokens JWT
- ❌ Rate limiting

Para produção, adicione:
```properties
# Kafka Security
spring.kafka.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required ...
```

## 📈 Performance

### Latência Esperada
- Produção de evento no Simulador: ~100ms
- Consumo no Cruzamento: ~50ms
- Processamento: ~100ms
- Produção de alerta: ~50ms
- Consumo no Orquestrador: ~50ms
- Processamento: ~100ms
- Produção de comando: ~50ms
- Consumo no Cruzamento: ~50ms
- Produção de status: ~50ms
- Consumo no Backend: ~50ms
- Broadcast WebSocket: ~100ms
- Render no Frontend: ~50ms
- **Total**: ~700-800ms (pode melhorar com otimizações)

### Escalabilidade
- Cada serviço pode ser escalado horizontalmente
- Aumentar `num.partitions` nos tópicos Kafka para paralelizar
- Usar load balancer para múltiplas instâncias

## 📚 Referências

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://spring.io/projects/spring-kafka)
- [FastAPI WebSocket](https://fastapi.tiangolo.com/advanced/websockets/)
- [React Flow](https://reactflow.dev/)
