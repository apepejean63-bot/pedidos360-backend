# Pedidos360 - Backend (DSY1107, EP1)

Dos microservicios Spring Boot:

- **`pedidos-service`** (puerto 8081): microservicio de dominio. CRUD de pedidos, conectado a MySQL, valida el JWT de Azure AD y autoriza por rol (`Admin`) para cambiar estado / eliminar.
- **`bff-service`** (puerto 8080): Backend For Frontend. Es el único punto de entrada que debería consumir el frontend Angular. Valida el mismo JWT y reenvía la petición a `pedidos-service`, propagando el token.

```
Angular (MSAL) --token--> BFF (8080) --token--> pedidos-service (8081) --> MySQL
                              ↑
                    (ambos validan el JWT de forma independiente)
```

En producción, el BFF (o ambos, según cómo lo configuren) queda detrás de **AWS API Gateway**, y las instancias corren en **EC2**.

## 1. Antes de correr nada: variables de entorno

Copia `.env.example` como `.env` y complétalo con los datos reales una vez que tengan el registro en Azure AD / Microsoft Entra ID (Tenant ID, Client ID del backend). Mientras tanto, la app **no va a levantar correctamente** si `AZURE_TENANT_ID` no es un tenant real, porque no puede descargar las llaves JWKS — eso es esperado, es la parte que falta configurar.

Cómo exportar las variables (ejemplo en Linux/Mac):
```bash
export AZURE_TENANT_ID=xxxxx
export AZURE_CLIENT_ID_BACKEND=xxxxx
export DB_HOST=localhost
export DB_USERNAME=root
export DB_PASSWORD=root
export PEDIDOS_SERVICE_URL=http://localhost:8081
```

En IntelliJ/Eclipse: configúralas en "Environment variables" de la configuración de Run de cada app.

## 2. Levantar MySQL local

```bash
docker compose up -d
```

## 3. Correr cada microservicio

```bash
cd pedidos-service && ./mvnw spring-boot:run
cd bff-service && ./mvnw spring-boot:run
```

## 4. Correr los tests

```bash
cd pedidos-service && ./mvnw test
cd bff-service && ./mvnw test
```

## 5. Pendiente por completar (una vez tengan Azure AD)

- [ ] Reemplazar `AZURE_TENANT_ID` y `AZURE_CLIENT_ID_BACKEND` con los valores reales
- [ ] Crear el App Role "Admin" en Azure AD y asignarlo a un usuario de prueba (necesario para probar `PATCH /estado` y `DELETE`)
- [ ] Conectar `pedidos-service` a una base de datos MySQL real en RDS (cambiar `DB_HOST`)
- [ ] Desplegar ambos JARs en instancias EC2
- [ ] Configurar AWS API Gateway apuntando al BFF (puerto 8080)
- [ ] Construir el frontend Angular con MSAL apuntando al BFF / API Gateway

## Endpoints expuestos (vía BFF, puerto 8080)

| Método | Endpoint                     | Requiere              |
|--------|-------------------------------|------------------------|
| GET    | `/api/v1/pedidos`             | Token JWT válido       |
| GET    | `/api/v1/pedidos/{id}`        | Token JWT válido       |
| POST   | `/api/v1/pedidos`              | Token JWT válido       |
| PATCH  | `/api/v1/pedidos/{id}/estado`  | Token JWT + rol `Admin`|
| DELETE | `/api/v1/pedidos/{id}`        | Token JWT + rol `Admin`|

Ejemplo de body para `POST /api/v1/pedidos`:
```json
{
  "cliente": "Juan Pérez",
  "email": "juan@example.com",
  "detalles": [
    { "producto": "Notebook", "cantidad": 1, "precioUnitario": 500000 }
  ]
}
```
