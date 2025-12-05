from kafka.admin import KafkaAdminClient, NewTopic
from kafka.errors import TopicAlreadyExistsError

def criar_topicos():
    admin_client = KafkaAdminClient(
        bootstrap_servers="localhost:9092",
        client_id='setup_gdt'
    )
    
    topic_list = [
        NewTopic(name="sistema.configuracao", num_partitions=1, replication_factor=1),
        NewTopic(name="sensor.veiculo", num_partitions=1, replication_factor=1),
        NewTopic(name="cruzamento.status", num_partitions=1, replication_factor=1),
        NewTopic(name="orquestrador.comando", num_partitions=1, replication_factor=1)
    ]
    
    try:
        admin_client.create_topics(new_topics=topic_list, validate_only=False)
        print("Tópicos criados com sucesso.")
    except TopicAlreadyExistsError as e:
        print(f"Um ou mais tópicos já existem: {e}")
    finally:
        admin_client.close()
        
if __name__ == "__main__":
    criar_topicos()