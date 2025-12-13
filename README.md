# GDT
Gestor Distribuido de tráfego tem como objetivo criar um gestor de tráfego inteligente usando métodos de sistemas distribuídos.

# Módulos
## Módulo Cruzamento
Segue ordens de fechar ou abrir o semáforo, vindas do orquestrador.
Monitora se há algum veículo esperando por muito tempo (1 minuto). Ele deve publicar eventos em um tópico.

## Módulo orquestrador
Analisa a situação atual das vias usando informações do sensor e envia comandos para os semáforos. Tentando manter um bom fluxo de veículos.

## Módulo visualizar
É uma interface gráfica para criar a simulação, observar e alterar parâmetros como a quantidade de veículos, adicionar novos cruzamentos e interligar vias.

## Módulo simulador
Um módulo para simular a geração de veículos. Este simulador é o que "ativa" os Módulos Sensores, criando os dados para o sistema reagir.
O veículo vai consultar o estado atual do grafo da cidade e procurar o caminho mais curto do ponto A para o ponto B, o ponto B será selecionado aleatoriamente.

# Como rodar o projeto




