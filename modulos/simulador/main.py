import asyncio
from core.engine import MotorSimulacao

if __name__ == "__main__":
    simulador = MotorSimulacao()
    try:
        asyncio.run(simulador.executar())
    except KeyboardInterrupt:
        print("Encerrando simulador...")