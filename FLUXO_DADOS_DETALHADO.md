# Fluxo de Dados Detalhado - Sistema de Gestão de Trânsito

## 📊 Visão Geral do Fluxo Completo

```
FRONTEND (React/Vite)
    ├─ Exibe interface gráfica com cruzamentos
    ├─ Permite criar/editar grafo viário
    ├─ Recebe atualizações via WebSocket
    └─ Mostra status dos semáforos em tempo real

    ↕ HTTP REST + WebSocket

BACKEND (FastAPI/Python)
    ├─ API REST para configuração
    ├─ Consumer Kafka (múltiplos tópicos)
    ├─ Producer Kafka (configuração)
    └─ WebSocket Manager (broadcast)

    ↕ Kafka

┌──────────────────────────────────────────┐
│        SERVIÇOS JAVA (Spring Boot)       │
├──────────────────────────────────────────┤
│                                          │
│  CRUZAMENTO          ORQUESTRADOR       │
│  ┌──────────────┐   ┌──────────────┐   │
│  │ Consumer:    │   │ Consumer:    │   │
│  │ - sensor.*   │   │ - alerta     │   │
│  │ - comando    │   │ - config     │   │
│  ├──────────────┤   ├──────────────┤   │
│  │ Producer:    │   │ Producer:    │   │
│  │ - status     │   │ - comando    │   │
│  │ - alerta     │   │              │   │
│  └──────────────┘   └──────────────┘   │
│         ↑ ↓                              │
└─────────┼──────────────────────────────┘
          │
      Kafka (5 tópicos)
          │
    ┌─────┴─────┐
    ↓           ↓
SIMULADOR    (Consumidores
(Python)      via WebSocket)
```

## 🔄 Ciclo de Vida Completo

### Fase 1: Inicialização (T=0s)

```
┌─ FRONTEND ────────────────────────────────┐
│ Usuário clica "Iniciar Simulação"        │
│ ├─ Grafo pronto (cruzamentos + arestas) │
│ ├─ Quantidade de veículos: 100          │
│ └─ Envia POST /configurar-simulacao     │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─ BACKEND ─────────────────────────────────┐
│ Recebe ConfiguracaoSimulacao             │
│ ├─ Cria SistemaConfig com DadosGrafo    │
│ ├─ Produz para Kafka topic:             │
│ │  "sistema.configuracao"               │
│ └─ Aguarda consumidores processarem     │
└──────────────┬──────────────────────────┘
               │
               ↓ Kafka
┌─ ORQUESTRADOR ────────────────────────────┐
│ @KafkaListener(topics="sistema.configuracao")
│ ├─ Recebe SistemaConfig                  │
│ ├─ OrquestradorService.configurarSistema()
│ ├─ Armazena DadosGrafo em memória       │
│ └─ Fica pronto para processar alertas   │
└───────────────────────────────────────────┘
```

### Fase 2: Simulação Ativa (T=1-5s)

```
┌─ SIMULADOR ────────────────────────────────┐
│ Iteração: T cada 500ms                    │
│ Para cada veículo:                        │
│ ├─ Calcula nova posição                  │
│ ├─ Identifica qual via está             │
│ └─ Produz SensorVeiculo para Kafka       │
│    Topic: "sensor.veiculo"                │
│    {                                      │
│      "id_veiculo": "v001",               │
│      "id_via": "1-2",     // 1 → 2      │
│      "posicao": 45.5,     // % da via   │
│      "velocidade": 30.0   // km/h       │
│    }                                      │
└──────────────┬──────────────────────────┘
               │
               ↓ Kafka
┌─ CRUZAMENTO ───────────────────────────────┐
│ @KafkaListener(topics="sensor.veiculo")   │
│                                           │
│ Para cada SensorVeiculo recebido:         │
│ ├─ Identifica qual cruzamento é afetado │
│ ├─ CruzamentoService.processarSensor()   │
│ ├─ Incrementa contador de fila          │
│ ├─ Verifica se fila > threshold (10)    │
│ └─ Se sim:                               │
│    └─ Produz CruzamentoAlerta            │
│       Topic: "cruzamento.alerta"         │
│       {                                   │
│         "id_cruzamento": "1",           │
│         "prioridade": "ALTA",            │
│         "tamanho_fila": 12               │
│       }                                   │
└──────────────┬──────────────────────────┘
               │
               ├─────────────────────┐
               ↓                     ↓
        ORQUESTRADOR           BACKEND
```

### Fase 3: Orquestração (T=5-10s)

```
┌─ ORQUESTRADOR ────────────────────────────┐
│ @KafkaListener(topics="cruzamento.alerta")│
│                                           │
│ Recebe CruzamentoAlerta (id_cruzamento=1)│
│                                           │
│ OrquestradorService.tratarAlerta():      │
│ ├─ Consulta DadosGrafo                   │
│ ├─ Verifica cruzamentos adjacentes       │
│ ├─ Determina ação (ABRIR/FECHAR/INVERTER)
│ ├─ Cria OrquestradorComando              │
│ ├─ Produz para Kafka topic:              │
│ │  "orquestrador.comando"                │
│ │  {                                      │
│ │    "id_cruzamento_alvo": "1",         │
│ │    "comando": "ABRIR",  // ou INVERTER│
│ │    "id_transacao": "CMD_123456"       │
│ │  }                                      │
│ └─ Aguarda Cruzamento executar          │
└──────────────┬──────────────────────────┘
               │
               ↓ Kafka
┌─ CRUZAMENTO ───────────────────────────────┐
│ @KafkaListener(                            │
│   topics="orquestrador.comando"           │
│ )                                          │
│                                           │
│ Recebe OrquestradorComando                │
│                                           │
│ CruzamentoService.executarComando():      │
│ ├─ Valida comando                         │
│ ├─ Altera status do semáforo             │
│ │  De: VERMELHO (N-S bloqueado)         │
│ │  Para: VERDE (N-S liberado)           │
│ │                                        │
│ │  Ou inverte:                          │
│ │  De: VERDE N-S, VERMELHO L-O         │
│ │  Para: VERMELHO N-S, VERDE L-O       │
│ ├─ Começa countdown de liberação        │
│ ├─ Produz CruzamentoStatus              │
│ │  Topic: "cruzamento.status"           │
│ │  {                                     │
│ │    "id_cruzamento": "1",             │
│ │    "status_sinal": "N-S",  // VERDE  │
│ │    "tamanho_fila": 8,  // diminuiu   │
│ │    "timestamp": 1234567890           │
│ │  }                                     │
│ └─ Veículos começam a sair da fila    │
└──────────────┬──────────────────────────┘
               │
               ↓ Kafka
```

### Fase 4: Visualização (T=10+s)

```
┌─ BACKEND ─────────────────────────────────┐
│ Kafka Consumer Thread:                   │
│                                          │
│ for message in consumer:                │
│ ├─ Recebe CruzamentoStatus              │
│ ├─ Cria payload JSON                    │
│ ├─ WebSocket Manager.broadcast()        │
│ └─ Envia para TODOS os clientes         │
│                                          │
│    payload = {                           │
│      "topic": "cruzamento.status",       │
│      "data": {                           │
│        "id_cruzamento": "1",            │
│        "status_sinal": "N-S",           │
│        "tamanho_fila": 8                │
│      }                                   │
│    }                                     │
└──────────────┬──────────────────────────┘
               │
               ↓ WebSocket (< 100ms)
┌─ FRONTEND ────────────────────────────────┐
│ ws.onmessage = (event) => {              │
│   const message = JSON.parse(event.data) │
│   handleSocketMessage(message)           │
│ }                                        │
│                                          │
│ handleSocketMessage():                   │
│ ├─ Extrai topic e data                  │
│ ├─ Se topic == "cruzamento.status":    │
│ │  ├─ Encontra nó com id_cruzamento   │
│ │  ├─ Atualiza node.data.status      │
│ │  │  (VERMELHO → VERDE)              │
│ │  ├─ Atualiza node.data.queueCount │
│ │  │  (12 → 8)                       │
│ │  └─ setNodes() → React re-render   │
│ │                                     │
│ └─ UI ATUALIZA com novo status         │
│                                        │
│ Visualização:                           │
│ ├─ Cruzamento 1: VERDE (semáforo)    │
│ ├─ Fila: 8 veículos                 │
│ ├─ Veículos saindo via "1-2"        │
│ └─ Animação de movimento no Reactflow│
└───────────────────────────────────────────┘
```

## 📈 Mapa Completo de Dados Entre Componentes

### Cruzamento Service

**Entrada (Consumer)**:
```
Topics consumidos:
├─ sensor.veiculo
│  └─ SensorVeiculo {id_veiculo, id_via, posicao, velocidade}
│
└─ orquestrador.comando
   └─ OrquestradorComando {id_cruzamento_alvo, comando}
```

**Processamento**:
```
CruzamentoConsumer
  └─ handleSensorVeiculo(SensorVeiculo)
     ├─ CruzamentoService.processarSensor()
     │  ├─ Identifica cruzamento destino
     │  ├─ Incrementa fila
     │  ├─ Se fila > threshold
     │  │  └─ gera alerta
     │  └─ Publica status do cruzamento
     │
     └─ handleComando(OrquestradorComando)
        └─ CruzamentoService.executarComando()
           ├─ Muda status do semáforo
           └─ Publica novo status
```

**Saída (Producer)**:
```
Topics produzidos:
├─ cruzamento.status (continuamente a cada sensor)
│  └─ CruzamentoStatus {id_cruzamento, status_sinal, tamanho_fila}
│
└─ cruzamento.alerta (quando fila > threshold)
   └─ CruzamentoAlerta {id_cruzamento, prioridade, tamanho_fila}
```

### Orquestrador Service

**Entrada (Consumer)**:
```
Topics consumidos:
├─ cruzamento.alerta
│  └─ CruzamentoAlerta {id_cruzamento, prioridade, tamanho_fila}
│
└─ sistema.configuracao
   └─ SistemaConfig {tipo_evento, dados_grafo}
```

**Processamento**:
```
DadosGrafo armazenado em memória:
├─ List<Cruzamento> cruzamentos
│  └─ Cruzamento {id, nome, latitude, longitude}
│
└─ List<Aresta> arestas
   └─ Aresta {origem, destino, distancia}

OrquestradorService:
├─ configurarSistema(SistemaConfig)
│  └─ Armazena DadosGrafo
│
└─ tratarAlerta(CruzamentoAlerta)
   ├─ Consulta grafo
   ├─ Determina próximo cruzamento
   ├─ Decide estratégia
   └─ Cria comando
```

**Saída (Producer)**:
```
Topics produzidos:
└─ orquestrador.comando
   └─ OrquestradorComando {id_cruzamento_alvo, comando}
```

## 🎬 Exemplo Prático Passo a Passo

### Cenário:
- 2 cruzamentos conectados: 1 → 2
- Via entre eles: 1-2 (100m)
- Velocidade média: 30 km/h (8.3 m/s) = 12s para atravessar
- Threshold de alerta: 10 veículos

### Timeline:

**T=0s**: Sistema inicia
- Simulador começa a produzir SensorVeiculo

**T=1s**: Primeiros veículos entram na via 1-2
- Simulador: produz v001, v002, v003 em sensor.veiculo
- Cruzamento: consome, incrementa fila para 3

**T=5s**: Acúmulo de veículos
- Simulador: v001 sai da via, vai para 2
- Cruzamento: fila agora = 11 (> 10)
- Cruzamento: produz alerta em cruzamento.alerta

**T=5.1s**: Orquestrador reage
- Orquestrador: consome alerta
- Orquestrador: consulta grafo, vê que 1→2
- Orquestrador: produz comando ABRIR em orquestrador.comando

**T=5.2s**: Cruzamento executa comando
- Cruzamento: consome comando
- Cruzamento: muda semáforo para VERDE
- Cruzamento: produz novo status em cruzamento.status

**T=5.3s**: Frontend recebe atualização
- Backend: consome status do Kafka
- Backend: faz broadcast via WebSocket
- Frontend: recebe mensagem
- Frontend: atualiza semáforo para VERDE
- **UI MUDA**: Usuário vê semáforo ficar verde!

**T=5.3-10s**: Veículos saem
- Enquanto semáforo está VERDE
- Veículos saem da fila (12s cada um)
- Fila diminui
- Cruzamento produz novos status
- Frontend atualiza fila em tempo real

**T=15s+**: Sistema se estabiliza
- Se fila < 10, semáforo volta ao controle de timing normal
- Ciclo recomeça

## 🔍 Verificação de Cada Estágio

```bash
# Estágio 1: Simulador produz
docker-compose logs simulador | grep "SensorVeiculo"

# Estágio 2: Cruzamento consome
docker-compose logs cruzamento | grep "sensor.veiculo"

# Estágio 3: Cruzamento produz alerta
docker-compose logs cruzamento | grep "alerta"

# Estágio 4: Orquestrador consome
docker-compose logs orquestrador | grep "alerta"

# Estágio 5: Orquestrador produz comando
docker-compose logs orquestrador | grep "comando"

# Estágio 6: Cruzamento consome comando
docker-compose logs cruzamento | grep "comando"

# Estágio 7: Cruzamento produz status
docker-compose logs cruzamento | grep "status"

# Estágio 8: Backend consome
docker-compose logs backend | grep "cruzamento.status"

# Estágio 9: Frontend recebe
# Abrir DevTools (F12) → Console → WebSocket
```

## 📊 Resumo de Tópicos e Fluxos

| Tópico | Produtor | Consumidor | Frequência | Tamanho |
|--------|----------|-----------|-----------|---------|
| `sensor.veiculo` | Simulador | Cruzamento, Backend | 10x/s | ~100 bytes |
| `cruzamento.status` | Cruzamento | Backend | ~cada sensor | ~150 bytes |
| `cruzamento.alerta` | Cruzamento | Orquestrador, Backend | quando fila>10 | ~120 bytes |
| `orquestrador.comando` | Orquestrador | Cruzamento | quando alerta | ~100 bytes |
| `sistema.configuracao` | Backend | Orquestrador | 1x no início | ~1KB+ |

## ⚡ Latência Esperada

```
Simulador gera evento
  ↓ (~5ms)
SensorVeiculo em Kafka
  ↓ (~50ms de lag Kafka)
Cruzamento consome
  ↓ (~50ms processamento)
CruzamentoStatus em Kafka
  ↓ (~50ms de lag Kafka)
Backend consome
  ↓ (~10ms processamento)
WebSocket broadcast
  ↓ (~100ms rede)
Frontend recebe
  ↓ (~10ms processamento)
React renderiza
  ↓ (~50ms render)
UI ATUALIZA

Total: ~375-425ms (menos de meio segundo!)
```
