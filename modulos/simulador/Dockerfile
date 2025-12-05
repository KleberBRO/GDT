# modulos/simulador/Dockerfile
FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .

# Instala as dependências (principalmente kafka-python)
RUN pip install --no-cache-dir -r requirements.txt

# Copia o código do simulador
COPY . .

# Comando para rodar o simulador
# Python -u faz o log aparecer instantaneamente no console do Docker
CMD ["python", "-u", "simulador.py"]