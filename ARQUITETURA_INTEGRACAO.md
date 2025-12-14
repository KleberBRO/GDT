# Integração Completa: Frontend, Backend e Serviços Java

## Arquitetura Geral

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Vue/React)                        │
│              (modulos/visualizar/frontend - Port 5173)             │
└────────────┬─────────────────────────────────────────────┬──────────┘
             │ HTTP/REST                   │ WebSocket
             │                             │
┌────────────▼─────────────────────────────▼──────────────────────────┐
│                    BACKEND (Python/FastAPI)                         │
│                (modulos/visualizar/backend - Port 8000)            │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │  APIs REST              WebSocket Manager                   │  │
│  │  - POST /configurar-    - Broadcast messages               │  │
│  │    simulacao            - Gerencia conexões                │  │
│  │  - GET /               └─────────────────────────────────────┘  │
│  └─────────────────────────────────────────────────────────────┘  │
│                           │                                         │
│                    Kafka Consumer                                  │
│                    Kafka Producer                                  │
└────────────┬─────────────────────────────────────────────────────┘
             │
             │ Kafka Messages (Topics: cruzamento.status, sensor.veiculo, etc.)
             │
     ┌───────┴────────┬────────────┬─────────────────┐
     │                │            │                 │
┌────▼────┐    ┌──────▼──┐  ┌──────▼──┐        ┌────▼────┐
│Orquestrador│  │Cruzamento│  │Simulador│        │  Kafka  │
│(Java)     │  │(Java)    │  │(Python) │        │ Broker  │
│Port 8082  │  │Port 8081 │  │(Docker) │        │ Port 9092│
└────────┬─────┴───┬──────┘  └────┬───┘        └────┬────┘
         │         │              │                 │
         └─────────┴──────────────┴─────────────────┘
                    Kafka Network
```

## Fluxo de Funcionamento

### 1. **Inicialização da Simulação**
```
Frontend → POST /configurar-simulacao → Backend → Kafka Producer
                                           ↓
Backend envia SistemaConfig para o tópico `sistema.configuracao`
```

### 2. **Consumo e Broadcast de Mensagens**
```
Kafka Consumer (Backend) → Consome de múltiplos tópicos
                           ├─ cruzamento.status
                           ├─ sensor.veiculo
                           └─ Qualquer outro tópico configurado
                                ↓
                        WebSocket Broadcast
                                ↓
Frontend recebe em tempo real via WebSocket
```

### 3. **Fluxo de Mensagens entre Serviços**

#### Simulador → Cruzamento:
- Simulador envia `SensorVeiculo` para `sensor.veiculo`
- Cruzamento consome de `sensor.veiculo`
- Cruzamento publica `CruzamentoAlerta` em `cruzamento.alerta`

#### Cruzamento → Orquestrador:
- Cruzamento publica `CruzamentoAlerta` em `cruzamento.alerta`
- Orquestrador consome de `cruzamento.alerta`
- Orquestrador publica `OrquestradorComando` em `orquestrador.comando`

#### Orquestrador → Cruzamento:
- Orquestrador publica `OrquestradorComando` em `orquestrador.comando`
- Cruzamento consome de `orquestrador.comando`

#### Qualquer → Backend (Visualização):
- Backend consome de tópicos monitorados (config em `app/config.py`)
- Backend faz broadcast via WebSocket para o Frontend

## Configuração Necessária

### 1. **Variáveis de Ambiente nos Serviços Java**
No `docker-compose.yml`:
```yaml
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
SPRING_APPLICATION_NAME: cruzamento  # ou orquestrador
```

### 2. **Tópicos Kafka a Serem Criados**
Os seguintes tópicos devem existir:
- `cruzamento.status` - Status dos cruzamentos
- `cruzamento.alerta` - Alertas enviados pelos cruzamentos
- `sensor.veiculo` - Posições dos veículos
- `orquestrador.comando` - Comandos do orquestrador
- `sistema.configuracao` - Configuração do sistema

### 3. **Configuração do Backend**
Em `app/config.py`, configure:
```python
TOPICS_TO_CONSUME = [
    'cruzamento.status',
    'sensor.veiculo',
    'cruzamento.alerta',
    'orquestrador.comando'
]
```

### 4. **CORS no Backend**
✓ Já configurado no `app/main.py` para aceitar todas as origens

## Fluxo Completo de uma Simulação

1. **Usuário clica "Iniciar Simulação" no Frontend**
   - Frontend chama `POST /configurar-simulacao` com:
     - DadosGrafo (cruzamentos e arestas)
     - Quantidade de veículos

2. **Backend recebe e envia para Kafka**
   - Envia `SistemaConfig` para tópico `sistema.configuracao`

3. **Orquestrador consome a configuração**
   - Recebe `SistemaConfig` via `@KafkaListener` em `sistema.configuracao`
   - Armazena a configuração do grafo

4. **Simulador inicia**
   - Começa a enviar `SensorVeiculo` para tópico `sensor.veiculo`

5. **Cruzamento consome dados do simulador**
   - Consome `SensorVeiculo` de `sensor.veiculo`
   - Analisa congestionamento
   - Publica `CruzamentoAlerta` em `cruzamento.alerta`

6. **Orquestrador processa alertas**
   - Consome `CruzamentoAlerta` de `cruzamento.alerta`
   - Decide qual comando enviar (ABRIR semáforo)
   - Publica `OrquestradorComando` em `orquestrador.comando`

7. **Cruzamento consome comando do orquestrador**
   - Consome `OrquestradorComando` de `orquestrador.comando`
   - Altera status do semáforo
   - Publica novo `CruzamentoStatus` em `cruzamento.status`

8. **Backend visualiza tudo**
   - Consome mensagens de todos os tópicos
   - Faz broadcast via WebSocket para o Frontend
   - Frontend atualiza a visualização em tempo real

## Checklist de Verificação

### Services Java
- [ ] Orquestrador tem `OrquestradorApplication.java` como classe principal
- [ ] Cruzamento tem `CruzamentoApplication.java` como classe principal
- [ ] Ambos conectam a `kafka:9092` (porta interna do Docker)
- [ ] `@KafkaListener` está configurado em ambos os serviços

### Backend Python
- [ ] `KAFKA_BROKER = "kafka:9092"` em `app/config.py`
- [ ] `TOPICS_TO_CONSUME` inclui todos os tópicos relevantes
- [ ] `CORSMiddleware` está configurado
- [ ] WebSocket está rodando em `/ws`

### Frontend
- [ ] WebSocket conecta a `ws://localhost:8000/ws` (ou seu domínio)
- [ ] `POST /configurar-simulacao` é chamado ao iniciar simulação
- [ ] Componentes React atualizam ao receber mensagens WebSocket

### Docker
- [ ] Zookeeper está rodando (porta 2181)
- [ ] Kafka está rodando (porta 9092 interna, 9092 externa para localhost)
- [ ] Backend está rodando (porta 8000)
- [ ] Frontend está rodando (porta 5173)
- [ ] Cruzamento está rodando (porta 8081)
- [ ] Orquestrador está rodando (porta 8082)
- [ ] Simulador está rodando

## URLs de Acesso

| Serviço | URL | Descrição |
|---------|-----|-----------|
| Frontend | http://localhost:5173 | Interface do usuário |
| Backend | http://localhost:8000 | API REST |
| Cruzamento | http://localhost:8081 | Serviço de cruzamentos (sem interface) |
| Orquestrador | http://localhost:8082 | Serviço de orquestração (sem interface) |
| Kafka | localhost:9092 | Broker de mensagens |

## Próximos Passos

1. **Estruturar código Java corretamente**
   - Criar pacotes Java apropriados para os serviços
   - Implementar `application.properties` com configurações Kafka

2. **Implementar aplicação properties**
   - Criar `application.properties` para cada serviço Java

3. **Testes de integração**
   - Testar comunicação Kafka entre serviços
   - Testar WebSocket entre Frontend e Backend
   - Testar fluxo completo de simulação

4. **Melhorias futuras**
   - Persistência de estado
   - Histórico de simulações
   - Logs detalhados
   - Metricas/Monitoring
