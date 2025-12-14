# GDT
Gestor Distribuido de tráfego tem como objetivo criar um gestor de tráfego inteligente usando métodos de sistemas distribuídos.

# Módulos
### Módulo Cruzamento
Segue ordens de fechar ou abrir o semáforo, vindas do orquestrador.
Monitora se há algum veículo esperando por muito tempo (1 minuto). Ele deve publicar eventos em um tópico.

### Módulo orquestrador
Analisa a situação atual das vias usando informações do sensor e envia comandos para os semáforos. Tentando manter um bom fluxo de veículos.

### Módulo visualizar
É uma interface gráfica para criar a simulação, observar e alterar parâmetros como a quantidade de veículos, adicionar novos cruzamentos e interligar vias.

### Módulo simulador
Um módulo para simular a geração de veículos. Este simulador é o que "ativa" os Módulos Sensores, criando os dados para o sistema reagir.
O veículo vai consultar o estado atual do grafo da cidade e procurar o caminho mais curto do ponto A para o ponto B, o ponto B será selecionado aleatoriamente.

# Como rodar o projeto
### pré requisitos
- ter docker instalado. sugestão: Docker Desktop
- 600 MB disponível (kafka é pesado)

### inicializar sistema
1. Vá para a pasta raiz do projeto
2. abra o cmd ou terminal
3. digitar o comando: `docker compose up -d --build`
4. esperar o zookeeper e kafka serem instalados e os módulos serem iniciados
5. entrar na página `http://localhost:5173/`



