# Pedidos360 - Backend (DSY1107)

Backend de Pedidos360 desarrollado con arquitectura de microservicios utilizando Spring Boot 3.3.4 (Java 17), MySQL, Kafka, RabbitMQ y autenticacion JWT con Microsoft Entra ID.

El frontend (Angular + MSAL) esta en `pedidos360-frontend` y la infraestructura (Kafka y RabbitMQ) en `pedidos360-infra`.

## Arquitectura

| Servicio | Puerto | Que hace |
|---|---|---|
| bff-service | 8080 | Backend For Frontend y punto de entrada para Angular. Valida y propaga el JWT |
| pedidos-service | 8081 | Gestiona pedidos (MySQL), seguridad JWT. Publica eventos en Kafka y RabbitMQ al crear un pedido |
| notificaciones-service | 8082 | Consume Kafka con 3 grupos: notificaciones, analitica y seguimiento |
| kafka-admin-service | 8083 | API de administracion de Kafka: topics, grupos, offsets y lag |
| rabbit-admin-service | 8084 | API de administracion de RabbitMQ: exchanges, colas con DLQ y bindings |

    Angular + MSAL (login con Microsoft Entra ID)
          |
          | JWT
          v
    BFF Service :8080
          |
          | JWT
          v
    Pedidos Service :8081 ----> MySQL :3307
          |            |
          |            +-----> Kafka: pedidos.eventos y pedidos.estado
          |                          |
          |                          v
          |                   Notificaciones Service :8082
          |                   (grupos: notificaciones, analitica, seguimiento)
          |
          +--> RabbitMQ (cluster rabbit1 + rabbit2)
                 pedidos.exchange (direct) -> pedidos.queue
                 pedidos.topic (topic)     -> notificaciones.queue, auditoria.queue
                 pedidos.dlx               -> pedidos.dlq, notificaciones.dlq, auditoria.dlq

    Kafka Admin Service :8083  -> AdminClient (topics, grupos y lag)
    Rabbit Admin Service :8084 -> exchanges, colas con DLQ y bindings

## Kafka

- Topics: `pedidos.eventos` (eventos del pedido) y `pedidos.estado` (estado actual, con compactacion). Ambos con 3 particiones y 3 replicas.
- Productor: clave = id del pedido (mantiene el orden por pedido), `acks=all` e idempotencia. Si Kafka falla, el pedido se guarda igual y el error queda registrado.
- Consumidores: DTO propio por topic, `ErrorHandlingDeserializer`, 3 reintentos (1 s) y luego el mensaje va al topic `.DLT` con sus cabeceras de error.
- Grupos: `notificaciones` (concurrencia 3, una particion por consumidor), `analitica` y `seguimiento`.
- Administracion: `kafka-admin-service` (AdminClient) y los scripts de `pedidos360-infra/kafka/scripts`.

### API de kafka-admin-service (puerto 8083)

| Metodo | Ruta | Descripcion |
|---|---|---|
| GET | /api/kafka/topics | Lista los topics |
| GET | /api/kafka/topics/{nombre} | Lider, replicas, ISR y configuracion |
| POST | /api/kafka/topics | Crea un topic |
| PUT | /api/kafka/topics/{nombre}/config | Cambia una configuracion (ej. retention.ms) |
| POST | /api/kafka/topics/{nombre}/partitions | Amplia las particiones |
| GET | /api/kafka/groups | Lista los grupos |
| GET | /api/kafka/groups/{id} | Miembros, offsets y lag |

## RabbitMQ

- Cluster de 2 nodos (`rabbit1` y `rabbit2`) con la politica `ha-todas`, que replica las colas en ambos.
- Exchange direct `pedidos.exchange` -> cola `pedidos.queue` (clave `pedidos.creado`).
- Exchange topic `pedidos.topic` -> `notificaciones.queue` (patron `pedido.*`) y `auditoria.queue` (patron `pedido.#`).
- Cada cola tiene su DLQ mediante el exchange `pedidos.dlx`: `pedidos.dlq`, `notificaciones.dlq` y `auditoria.dlq`.
- Confirmacion manual (ACK/NACK). Un mensaje con `ERROR_TEST` termina en la DLQ.
- API de administracion en `rabbit-admin-service`: `/api/v1/rabbit-admin/exchanges`, `/queues` y `/bindings`.

## Como ejecutarlo en local

Requisitos: Docker Desktop, Java 17, Maven y Node.js.

1. Variables de entorno: ver `.env.example` y definir `AZURE_TENANT_ID` y `AZURE_CLIENT_ID_BACKEND`.
2. Base de datos: `docker compose up -d mysql` en este repositorio.
3. Kafka (repo `pedidos360-infra`, carpeta `kafka`):

        docker compose -f docker-compose.kafka.yml up -d
        docker compose -f docker-compose.kafka-ui.yml up -d

4. RabbitMQ (repo `pedidos360-infra`, carpeta `rabbitmq`):

        docker compose -f docker-compose.rabbitmq.yml up -d
        .\scripts\cluster-unir.ps1
        .\scripts\politica-ha.ps1

5. Cada servicio, en su carpeta:

        mvn spring-boot:run

6. Frontend: `npm start` en `pedidos360-frontend` y abrir `http://localhost:4200`.

Kafka UI: `http://127.0.0.1:8090`. RabbitMQ: `http://127.0.0.1:15672` (usuario y clave de desarrollo en el compose).

## Pruebas

    mvn test

Cada servicio tiene sus pruebas (por ejemplo, 11 en notificaciones-service y 7 en kafka-admin-service).