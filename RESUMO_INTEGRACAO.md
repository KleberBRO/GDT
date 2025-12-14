# Resumo: Integração do Frontend com os Serviços Java

## 📋 O que foi criado

### 1. **Arquitetura Completa** (`ARQUITETURA_INTEGRACAO.md`)
Documento que explica:
- Diagrama da arquitetura geral
- Fluxo de funcionamento entre Frontend, Backend e Serviços Java
- Fluxo completo de uma simulação (passo a passo)
- Checklist de verificação
- URLs de acesso

### 2. **Quick Start Guide** (`QUICK_START.md`)
Guia prático para iniciar rapidamente:
- Como clonar e executar o projeto
- Como usar a aplicação
- Como verificar logs
- Solução de problemas comuns

### 3. **Detalhes Técnicos** (`INTEGRACAO_TECNICA.md`)
Documentação técnica aprofundada:
- Estrutura das conexões WebSocket e REST
- Especificação de cada tópico Kafka
- Arquitetura dos serviços Java
- Ciclo completo de uma simulação (com timestamps)
- Debugging e troubleshooting
- Performance e escalabilidade

### 4. **Application Properties**
Arquivos de configuração Spring Boot:
- `modulos/cruzamento/src/main/resources/application.properties`
- `modulos/orquestrador/src/main/resources/application.properties`

### 5. **Application Classes**
Classes principais Java:
- `modulos/cruzamento/CruzamentoApplication.java`
- `modulos/orquestrador/OrquestradorApplication.java`

## 🔄 Como Tudo Funciona

```
┌─────────────────┐
│  Frontend       │ (React/Vite)
│  Port 5173      │
└────────┬────────┘
         │
    HTTP │ WebSocket
    /REST│
         │
┌────────▼──────────┐
│  Backend          │ (FastAPI)
│  Port 8000        │
└────┬─────────┬────┘
     │         │
     │ Kafka   │
     │         │
 ┌───▼──┐  ┌──▼───┐
 │      │  │      │
┌──────┴──┴──────┐
│ Cruzamento     │ (Spring Boot)  ←→  Orquestrador (Spring Boot)
│ Port 8081      │                    Port 8082
└────────────────┘
     ↑ ↓
    Simulador
    (Python)
```

### Fluxo de Dados:

1. **Frontend → Backend (REST)**
   - POST `/configurar-simulacao` com grafo e veículos

2. **Backend → Kafka Producer**
   - Envia `SistemaConfig` para tópico `sistema.configuracao`

3. **Simulador → Kafka**
   - Envia `SensorVeiculo` para tópico `sensor.veiculo`

4. **Cruzamento Service**
   - Consome `SensorVeiculo` e `OrquestradorComando`
   - Produz `CruzamentoAlerta` e `CruzamentoStatus`

5. **Orquestrador Service**
   - Consome `CruzamentoAlerta` e `SistemaConfig`
   - Produz `OrquestradorComando`

6. **Backend → Kafka Consumer**
   - Consome todos os tópicos
   - Faz broadcast via WebSocket

7. **Kafka Consumer → Frontend (WebSocket)**
   - Envia atualizações em tempo real
   - Frontend renderiza estado dos cruzamentos

## ✅ O que foi implementado

- ✅ Dockerfiles para Cruzamento e Orquestrador com suporte a múltiplos módulos Java
- ✅ Configuração do docker-compose com todos os serviços
- ✅ Classes de aplicação Spring Boot (`*Application.java`)
- ✅ Configuração de propriedades Spring Boot (Kafka, logging)
- ✅ `pom.xml` correto para o Orquestrador
- ✅ Correção no `pom.xml` do Contratos (skip Spring Boot repackage)
- ✅ Documentação completa de integração
- ✅ Guias de quick start e debugging

## 🚀 Próximas Etapas Recomendadas

### Imediato
1. Testar se todos os containers iniciam:
   ```bash
   docker-compose up -d --build
   docker-compose ps
   ```

2. Verificar conectividade:
   ```bash
   curl http://localhost:8000/
   docker-compose logs -f backend
   ```

3. Testar frontend:
   ```bash
   # Abrir http://localhost:5173
   # Tentar criar cruzamentos e iniciar simulação
   ```

### Curto Prazo
1. Implementar classes Java faltantes em cada serviço
   - Enums (StatusSinal, OrquestradorComandos)
   - Models (Cruzamento, etc.)
   - Repositories
   - Consumer e Producer classes

2. Criar `application.properties` para o Contratos se necessário

3. Implementar lógica de negócio nos serviços

4. Testar fluxo completo Kafka

### Médio Prazo
1. Adicionar persistência (banco de dados)
2. Implementar autenticação/autorização
3. Adicionar testes unitários
4. Implementar metrics/monitoring
5. Otimizar performance (latência)

## 📚 Documentos Criados

| Documento | Localização | Descrição |
|-----------|------------|-----------|
| Arquitetura de Integração | `ARQUITETURA_INTEGRACAO.md` | Visão geral da arquitetura |
| Quick Start | `QUICK_START.md` | Como iniciar e usar |
| Detalhes Técnicos | `INTEGRACAO_TECNICA.md` | Especificações técnicas |
| Properties (Cruzamento) | `modulos/cruzamento/.../application.properties` | Config Spring Boot |
| Properties (Orquestrador) | `modulos/orquestrador/.../application.properties` | Config Spring Boot |

## 🎯 Resumo Técnico

### Comunicação Entre Camadas

**Frontend ↔ Backend**: HTTP REST + WebSocket
- Frontend envia configurações via REST
- Backend envia atualizações via WebSocket

**Backend ↔ Serviços Java**: Kafka
- Backend produz eventos de configuração
- Backend consome eventos de status
- Serviços Java se comunicam via Kafka

**Serviços Java**: Kafka
- Cruzamento consome sensor e comandos
- Orquestrador consome alertas e config
- Ambos produzem eventos para outros

### Tópicos Kafka (5 tópicos)

1. `sensor.veiculo` - Simulador → Cruzamento
2. `cruzamento.alerta` - Cruzamento → Orquestrador
3. `cruzamento.status` - Cruzamento → Backend
4. `orquestrador.comando` - Orquestrador → Cruzamento
5. `sistema.configuracao` - Backend → Orquestrador

## 💡 Exemplo Prático

1. Usuário abre http://localhost:5173
2. Clica "Adicionar Cruzamento" 2x
3. Conecta cruzamentos com drag-drop
4. Define 100 veículos
5. Clica "Iniciar Simulação"
6. Sistema envia grafo para Kafka
7. Simulador começa a gerar eventos
8. Cruzamentos processam veículos
9. Frontend recebe atualizações via WebSocket
10. Visualização em tempo real de semáforos e filas

## 📞 Suporte

Verifique:
- `ARQUITETURA_INTEGRACAO.md` - Para entender a arquitetura
- `QUICK_START.md` - Para problemas com execução
- `INTEGRACAO_TECNICA.md` - Para detalhes técnicos
- Logs via `docker-compose logs <serviço>`
