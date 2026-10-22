# Pedidos360 - Backend (DSY1107)

Backend de Pedidos360 desarrollado con arquitectura de microservicios utilizando Spring Boot, MySQL, RabbitMQ y autenticacion JWT con Microsoft Entra ID.

## Arquitectura

El backend esta compuesto por tres microservicios:

- **bff-service (8080):** Backend For Frontend y punto de entrada para Angular. Valida y propaga el JWT.
- **pedidos-service (8081):** gestiona pedidos, MySQL, seguridad JWT y mensajeria RabbitMQ.
- **rabbit-admin-service (8082):** administra queues, exchanges y bindings de RabbitMQ mediante API REST.

```text
Angular + MSAL
      |
      | JWT
      v
BFF Service :8080
      |
      | JWT
      v
Pedidos Service :8081
      |
      +---------------------> MySQL :3307
      |
      v
RabbitMQ :5672
      |
      +--> pedidos.exchange
      |         |
      |         | pedidos.creado
      |         v
      |     pedidos.queue
      |
      +--> pedidos.dlx
                |
                v
            pedidos.dlq

Rabbit Admin Service :8082
      |
      +--> Queues
      +--> Exchanges
      +--> Bindings