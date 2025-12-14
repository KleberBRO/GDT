# Quick Start Guide - Sistema de Gestão de Trânsito

## 📋 Pré-requisitos

- Docker e Docker Compose instalados
- Git configurado

## 🚀 Iniciando o Projeto

### Passo 1: Clone e acesse o repositório
```bash
cd /workspaces/GDT
```

### Passo 2: Inicie todos os serviços
```bash
docker-compose up -d --build
```

Isso iniciará:
- ✅ Zookeeper (porta 2181)
- ✅ Kafka (porta 9092)
- ✅ Backend Python/FastAPI (porta 8000)
- ✅ Frontend React/Vite (porta 5173)
- ✅ Cruzamento Java (porta 8081)
- ✅ Orquestrador Java (porta 8082)
- ✅ Simulador Python (Docker)

### Passo 3: Verifique o status
```bash
docker-compose ps
```

Todos os containers devem estar com status `Up`.

### Passo 4: Acesse a aplicação
Abra no navegador: **http://localhost:5173**

## 🎮 Como Usar

### 1. **Criar Cruzamentos**
- Clique no botão "Adicionar Cruzamento"
- Posicione os cruzamentos no canvas

### 2. **Conectar Cruzamentos**
- Arraste de um cruzamento para outro para criar uma via
- Isso cria as arestas do grafo

### 3. **Configurar Simulação**
- Defina a quantidade de veículos
- Clique em "Iniciar Simulação"

### 4. **Monitorar em Tempo Real**
- O frontend recebe atualizações via WebSocket
- Veja o status dos semáforos mudando
- Acompanhe o tamanho das filas

## 🔍 Verificação de Logs

### Backend Python
```bash
docker-compose logs -f backend
```

### Cruzamento Java
```bash
docker-compose logs -f cruzamento
```

### Orquestrador Java
```bash
docker-compose logs -f orquestrador
```

### Kafka
```bash
docker-compose logs -f kafka
```

## 📊 Tópicos Kafka Criados

Os seguintes tópicos serão utilizados:

| Tópico | Produtor | Consumidor | Descrição |
|--------|----------|-----------|-----------|
| `sensor.veiculo` | Simulador | Cruzamento, Backend | Posição dos veículos |
| `cruzamento.alerta` | Cruzamento | Orquestrador, Backend | Alertas de congestionamento |
| `cruzamento.status` | Cruzamento | Backend | Status atual dos cruzamentos |
| `orquestrador.comando` | Orquestrador | Cruzamento | Comandos para alterar semáforos |
| `sistema.configuracao` | Backend | Orquestrador | Configuração do sistema |

## 🛑 Parar os Serviços

```bash
docker-compose down
```

Para remover volumes também:
```bash
docker-compose down -v
```

## ❌ Solução de Problemas

### Conexão recusada em localhost:5173
- Verifique se o frontend está rodando: `docker-compose logs frontend`
- Aguarde 30 segundos para a build terminar

### Kafka não conecta
- Verifique se Zookeeper está rodando primeiro
- Aguarde 15 segundos após iniciar o Zookeeper
- Verifique: `docker-compose logs kafka`

### Backend não conecta ao Kafka
- Verifique se Kafka está pronto: `docker-compose logs kafka`
- Verifique logs do backend: `docker-compose logs backend`
- O backend tenta reconectar automaticamente a cada 5 segundos

### Frontend não recebe mensagens
- Verifique conexão WebSocket no DevTools (F12 → Network → WS)
- Verifique se o simulador está rodando: `docker-compose logs simulador`

## 📝 Estrutura do Projeto

```
GDT/
├── modulos/
│   ├── contratos/           # Modelos e contratos Kafka
│   ├── cruzamento/          # Serviço de Cruzamento (Java)
│   ├── orquestrador/        # Serviço de Orquestrador (Java)
│   ├── simulador/           # Simulador de Veículos (Python)
│   └── visualizar/
│       ├── backend/         # API e WebSocket (Python)
│       └── frontend/        # Interface (React/Vite)
├── docker-compose.yml
└── ARQUITETURA_INTEGRACAO.md
```

## 🤝 Contribuindo

1. Crie uma branch para sua feature: `git checkout -b feature/MinhaFeature`
2. Commit suas mudanças: `git commit -m 'Add MinhaFeature'`
3. Push para a branch: `git push origin feature/MinhaFeature`
4. Abra um Pull Request

## 📚 Documentação Adicional

- [Arquitetura Completa](./ARQUITETURA_INTEGRACAO.md) - Detalhes técnicos
- [README Simulador](./modulos/simulador/README.md)
- [README Backend](./modulos/visualizar/backend/README.md)
- [README Frontend](./modulos/visualizar/frontend/README.md)
