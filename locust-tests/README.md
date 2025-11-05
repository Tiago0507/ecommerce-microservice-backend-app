# Documentación de Pruebas de Rendimiento con Locust

## Descripción General

Las pruebas de rendimiento del proyecto se implementan utilizando Locust, un framework de código abierto para pruebas de carga y estrés. Estas pruebas simulan el comportamiento de múltiples usuarios concurrentes interactuando con la plataforma de comercio electrónico, permitiendo identificar cuellos de botella, medir la capacidad del sistema y validar el rendimiento bajo diferentes condiciones de carga.

## Propósito de las Pruebas de Rendimiento

Las pruebas de carga cumplen los siguientes objetivos:

- **Validar capacidad**: Determinar cuántos usuarios concurrentes puede soportar el sistema
- **Identificar límites**: Encontrar el punto de ruptura del sistema bajo estrés
- **Medir tiempos de respuesta**: Verificar que los endpoints respondan dentro de límites aceptables
- **Detectar cuellos de botella**: Identificar servicios o recursos que limitan el rendimiento
- **Simular patrones reales**: Reproducir comportamientos de usuarios reales en el sistema
- **Validar escalabilidad**: Verificar cómo responde el sistema al aumentar la carga
- **Probar integración**: Validar el comportamiento de microservicios bajo carga concurrente

## Estructura del Proyecto

```
locust-tests/
├── locustfile.py              # Archivo principal con flujos de usuario
├── requirements.txt           # Dependencias de Python
├── config/
│   └── test_config.py         # Configuración centralizada
├── tasks/
│   ├── user_tasks.py          # Tareas del User Service
│   ├── product_tasks.py       # Tareas del Product Service
│   ├── order_tasks.py         # Tareas del Order Service
│   ├── payment_tasks.py       # Tareas del Payment Service
│   └── favourite_tasks.py     # Tareas del Favourite Service
├── reports/                   # Reportes generados automáticamente
├── run_load_test.sh          # Script para prueba de carga
├── run_stress_test.sh        # Script para prueba de estrés
└── run_spike_test.sh         # Script para prueba de picos
```

## Arquitectura de las Pruebas

### Diseño Modular

Las pruebas se organizan en módulos independientes por microservicio:

- **user_tasks.py**: Operaciones de gestión de usuarios
- **product_tasks.py**: Operaciones de catálogo de productos
- **order_tasks.py**: Operaciones de pedidos y carritos
- **payment_tasks.py**: Operaciones de procesamiento de pagos
- **favourite_tasks.py**: Operaciones de favoritos

Cada módulo contiene tareas específicas que se combinan en el archivo principal `locustfile.py`.

### Clase Principal de Usuario

```python
class EcommerceUser(FastHttpUser):
    """
    Usuario simulado que representa un cliente del e-commerce.
    Utiliza FastHttpUser para mejor rendimiento.
    """
    tasks = [EcommerceTasks]
    wait_time = between(1, 3)  # Pausa entre 1-3 segundos entre tareas
    host = "http://localhost:8900"  # API Gateway
```

### Sistema de Tareas

Las tareas se definen con decoradores que especifican:

- **@task(peso)**: Frecuencia relativa de ejecución
- **@tag('categoría')**: Etiquetas para filtrado selectivo

Ejemplo:

```python
@task(5)  # Se ejecuta 5 veces más frecuente que una tarea con peso 1
@tag('user', 'read')
def get_users(self):
    """Obtiene la lista de usuarios"""
    self.client.get("/app/api/users", name="[USER] Get all users")
```

## Tipos de Pruebas

### Prueba de Carga (Load Test)

Simula una carga constante y sostenida en el tiempo para evaluar el comportamiento del sistema bajo condiciones normales de operación.

**Parámetros predeterminados:**
- **Usuarios**: 100 usuarios concurrentes
- **Tasa de generación**: 10 usuarios/segundo
- **Duración**: 3 minutos

**Objetivo**: Validar que el sistema funciona correctamente bajo carga operacional esperada.

### Prueba de Estrés (Stress Test)

Incrementa progresivamente la carga hasta encontrar el punto de ruptura del sistema, superando la capacidad operacional normal.

**Parámetros predeterminados:**
- **Usuarios**: 500 usuarios concurrentes
- **Tasa de generación**: 50 usuarios/segundo
- **Duración**: 2 minutos

**Objetivo**: Identificar los límites del sistema y cómo se degrada bajo sobrecarga.

### Prueba de Picos (Spike Test)

Simula un aumento súbito y masivo de usuarios en un corto período de tiempo para evaluar la capacidad del sistema de manejar picos de tráfico inesperados.

**Parámetros predeterminados:**
- **Usuarios**: 1000 usuarios concurrentes
- **Tasa de generación**: 100 usuarios/segundo
- **Duración**: 1 minuto

**Objetivo**: Verificar la respuesta del sistema ante aumentos repentinos de tráfico.

## Flujos de Usuario Implementados

### Flujo de Navegación de Usuario

Simula un usuario navegando el catálogo y agregando productos a favoritos.

**Secuencia de operaciones:**
1. Consulta el catálogo de productos
2. Visualiza detalles de 2-3 productos aleatorios
3. Agrega un producto a favoritos

**Peso de tarea**: 3 (ejecutado frecuentemente)
**Tags**: `integration`, `flow`, `critical`

```python
@task(3)
@tag('integration', 'flow', 'critical')
def user_browsing_flow(self):
    # 1. Navegar catálogo
    self.client.get("/app/api/products")
    
    # 2. Ver detalles de productos
    for _ in range(random.randint(2, 3)):
        product_id = random.choice(self.user.product_ids)
        self.client.get(f"/app/api/products/{product_id}")
    
    # 3. Agregar a favoritos
    self.client.post("/app/api/favourites", json=payload)
```

### Flujo Completo de Compra

Simula el proceso completo desde la creación de un pedido hasta el envío.

**Secuencia de operaciones:**
1. Crear pedido
2. Procesar pago
3. Crear registro de envío

**Peso de tarea**: 1 (menos frecuente, operación crítica)
**Tags**: `integration`, `flow`, `critical`

```python
@task(1)
@tag('integration', 'flow', 'critical')
def complete_purchase_flow(self):
    # 1. Crear orden
    order_response = self.client.post("/app/api/orders", json=order_payload)
    
    if order_response.status_code in [200, 201]:
        # 2. Procesar pago
        payment_response = self.client.post("/app/api/payments", json=payment_payload)
        
        if payment_response.status_code in [200, 201]:
            # 3. Crear envío
            self.client.post("/app/api/shippings", json=shipping_payload)
```

### Flujo de Carrito a Checkout

Simula la creación de un carrito, agregado de productos y proceso de checkout.

**Secuencia de operaciones:**
1. Crear carrito de compras
2. Visualizar productos
3. Proceder al checkout (crear orden)

**Peso de tarea**: 2
**Tags**: `integration`, `flow`

## Tareas por Microservicio

### User Service

**Operaciones de lectura:**
- `get_users`: Listar todos los usuarios (peso: 5)
- `get_user_by_id`: Obtener usuario específico (peso: 6)

**Operaciones de escritura:**
- `create_user`: Crear nuevo usuario (peso: 1)
- `update_user`: Actualizar usuario existente (peso: 2)

### Product Service

**Operaciones de lectura:**
- `get_products`: Listar todos los productos (peso: 10) - Operación más frecuente
- `get_product_by_id`: Obtener producto específico (peso: 8)

**Operaciones de escritura:**
- `create_product`: Crear nuevo producto (peso: 2)
- `update_product`: Actualizar producto existente (peso: 3)

### Order Service

**Operaciones de lectura:**
- `get_orders`: Listar todos los pedidos (peso: 4)
- `get_order_by_id`: Obtener pedido específico (peso: 5)
- `get_carts`: Listar carritos de compra (peso: 3)

**Operaciones de escritura:**
- `create_order`: Crear nuevo pedido (peso: 2)
- `create_cart`: Crear carrito de compras (peso: 1)

### Payment Service

**Operaciones de lectura:**
- `get_payments`: Listar todos los pagos (peso: 4)
- `get_payment_by_id`: Obtener pago específico (peso: 5)

**Operaciones de escritura:**
- `create_payment`: Procesar pago (peso: 2, tag: `critical`)
- `update_payment`: Actualizar estado de pago (peso: 1)

### Favourite Service

**Operaciones de lectura:**
- `get_favourites`: Listar favoritos (peso: 4)
- `get_favourite_by_id`: Obtener favorito específico (peso: 3)

**Operaciones de escritura:**
- `create_favourite`: Agregar a favoritos (peso: 2)
- `delete_favourite`: Eliminar de favoritos (peso: 1)

## Configuración de Pruebas

### Archivo test_config.py

Centraliza toda la configuración de las pruebas:

```python
class TestConfig:
    # Configuración del API Gateway
    API_GATEWAY_HOST = "http://localhost:8900"
    API_BASE_PATH = "/app/api"
    
    # Rangos de datos de prueba
    PRODUCT_IDS = [1, 2, 3, 4]
    USER_IDS = list(range(1, 51))
    ORDER_IDS = list(range(1, 201))
    PAYMENT_IDS = list(range(1, 150))
    
    # Simulación de comportamiento de usuario
    MIN_WAIT_TIME = 1  # segundos
    MAX_WAIT_TIME = 3  # segundos
    
    # Parámetros de prueba de carga
    LOAD_TEST_USERS = 100
    LOAD_TEST_SPAWN_RATE = 10
    LOAD_TEST_DURATION = "10m"
    
    # Parámetros de prueba de estrés
    STRESS_TEST_USERS = 500
    STRESS_TEST_SPAWN_RATE = 50
    STRESS_TEST_DURATION = "5m"
    
    # Parámetros de prueba de picos
    SPIKE_TEST_USERS = 1000
    SPIKE_TEST_SPAWN_RATE = 100
    SPIKE_TEST_DURATION = "3m"
```

### Datos de Prueba

Las pruebas utilizan rangos específicos de IDs que existen en la base de datos:

- **Productos**: IDs 1-4 (productos existentes en base de datos)
- **Usuarios**: IDs 1-50
- **Pedidos**: IDs 1-200
- **Pagos**: IDs 1-150

Estos datos se seleccionan aleatoriamente durante la ejecución para simular patrones de acceso realistas.

## Instalación y Configuración

### Requisitos Previos

- Python 3.8 o superior
- pip (gestor de paquetes de Python)
- Sistema operativo: Linux, macOS o Windows
- Sistema de e-commerce ejecutándose (todos los microservicios activos)

### Instalación

#### Paso 1: Crear Entorno Virtual

```bash
cd locust-tests
python3 -m venv venv
```

#### Paso 2: Activar Entorno Virtual

En Linux/macOS:
```bash
source venv/bin/activate
```

En Windows:
```bash
venv\Scripts\activate
```

#### Paso 3: Instalar Dependencias

```bash
pip install -r requirements.txt
```

Esto instala:
- **locust**: Framework de pruebas de carga (versión 2.32.0)

### Verificación de Instalación

```bash
locust --version
```

Debe mostrar:
```
locust 2.32.0
```

## Ejecución de Pruebas

### Prerequisitos para Ejecutar Pruebas

Antes de ejecutar las pruebas, asegurar que:

1. Todos los microservicios estén ejecutándose
2. El API Gateway esté disponible en `http://localhost:8900`
3. La base de datos tenga datos de prueba
4. Los servicios de infraestructura estén activos (Eureka, Config Server)

### Modo Interactivo (UI Web)

Ejecutar Locust con interfaz web para control manual:

```bash
cd locust-tests
source venv/bin/activate
locust -f locustfile.py --host=http://localhost:8900
```

Luego abrir el navegador en `http://localhost:8089` y configurar:
- Número de usuarios
- Tasa de generación (spawn rate)
- Host (si no se especificó en línea de comandos)

La interfaz web proporciona:
- Gráficas en tiempo real de solicitudes por segundo
- Tiempos de respuesta
- Número de usuarios activos
- Estadísticas de éxito/fallo por endpoint

### Prueba de Carga Automatizada

Ejecutar usando el script predefinido:

```bash
cd locust-tests
./run_load_test.sh
```

Este script ejecuta:
- 100 usuarios concurrentes
- 10 usuarios/segundo de generación
- Duración de 3 minutos
- Genera reporte HTML y CSV automáticamente

**Ejecución manual equivalente:**

```bash
locust -f locustfile.py \
    --headless \
    --users 100 \
    --spawn-rate 10 \
    --run-time 3m \
    --host http://localhost:8900 \
    --html reports/load-test-$(date +%Y%m%d-%H%M%S).html \
    --csv reports/load-test-$(date +%Y%m%d-%H%M%S)
```

### Prueba de Estrés Automatizada

Ejecutar usando el script predefinido:

```bash
cd locust-tests
./run_stress_test.sh
```

Este script ejecuta:
- 500 usuarios concurrentes
- 50 usuarios/segundo de generación
- Duración de 2 minutos

**Ejecución manual equivalente:**

```bash
locust -f locustfile.py \
    --headless \
    --users 500 \
    --spawn-rate 50 \
    --run-time 2m \
    --host http://localhost:8900 \
    --html reports/stress-test-$(date +%Y%m%d-%H%M%S).html \
    --csv reports/stress-test-$(date +%Y%m%d-%H%M%S)
```

### Prueba de Picos Automatizada

Ejecutar usando el script predefinido:

```bash
cd locust-tests
./run_spike_test.sh
```

Este script ejecuta:
- 1000 usuarios concurrentes
- 100 usuarios/segundo de generación
- Duración de 1 minuto

**Ejecución manual equivalente:**

```bash
locust -f locustfile.py \
    --headless \
    --users 1000 \
    --spawn-rate 100 \
    --run-time 1m \
    --host http://localhost:8900 \
    --html reports/spike-test-$(date +%Y%m%d-%H%M%S).html \
    --csv reports/spike-test-$(date +%Y%m%d-%H%M%S)
```

### Ejecución con Filtros por Tags

Ejecutar solo tareas específicas usando tags:

```bash
# Solo operaciones de lectura
locust -f locustfile.py --tags read --users 50 --spawn-rate 5

# Solo operaciones críticas
locust -f locustfile.py --tags critical --users 30 --spawn-rate 3

# Solo servicio de productos
locust -f locustfile.py --tags product --users 100 --spawn-rate 10

# Múltiples tags
locust -f locustfile.py --tags product,user --users 75 --spawn-rate 7
```

### Ejecución Distribuida (Múltiples Nodos)

Para pruebas de mayor escala, ejecutar Locust en modo distribuido:

**Nodo maestro:**
```bash
locust -f locustfile.py --master --expect-workers=3
```

**Nodos trabajadores:**
```bash
locust -f locustfile.py --worker --master-host=localhost
```

Esto permite distribuir la carga entre múltiples máquinas.

### Parámetros Personalizados

Ejecutar con configuración personalizada:

```bash
locust -f locustfile.py \
    --headless \
    --users 200 \
    --spawn-rate 20 \
    --run-time 5m \
    --host http://localhost:8900 \
    --stop-timeout 60
```

Parámetros disponibles:
- `--users`: Número total de usuarios concurrentes
- `--spawn-rate`: Usuarios generados por segundo
- `--run-time`: Duración total (ej: 10s, 5m, 2h)
- `--host`: URL base del sistema a probar
- `--headless`: Modo sin interfaz web
- `--html`: Archivo de salida para reporte HTML
- `--csv`: Prefijo para archivos CSV de resultados
- `--stop-timeout`: Tiempo de espera para detener usuarios (segundos)

## Interpretación de Resultados

### Salida en Consola

Durante la ejecución, Locust muestra estadísticas en tiempo real:

```
Type     Name                                    # reqs      # fails  |     Avg     Min     Max  Median  |   req/s failures/s
--------|----------------------------------------|-------|-------------|-------|-------|-------|--------|---------|-----------
GET      [PRODUCT] Get all products                 1250     0(0.00%)  |      45      12     234      38  |   41.67       0.00
GET      [PRODUCT] Get product by ID                1100     3(0.27%)  |      52      15     312      42  |   36.67       0.10
POST     [ORDER] Create order                        250     1(0.40%)  |     123      45     567     110  |    8.33       0.03
GET      [USER] Get all users                        450     0(0.00%)  |      38      10     189      32  |   15.00       0.00
--------|----------------------------------------|-------|-------------|-------|-------|-------|--------|---------|-----------
         Aggregated                                 3050     4(0.13%)  |      58      10     567      40  |  101.67       0.13
```

**Columnas importantes:**

- **# reqs**: Total de solicitudes realizadas
- **# fails**: Solicitudes fallidas (y porcentaje)
- **Avg**: Tiempo de respuesta promedio (ms)
- **Min/Max**: Tiempo mínimo y máximo de respuesta
- **Median**: Mediana de tiempos de respuesta
- **req/s**: Solicitudes por segundo
- **failures/s**: Fallos por segundo

### Reporte HTML

Los reportes HTML generados contienen:

1. **Resumen estadístico**: Tabla con métricas por endpoint
2. **Gráficas de rendimiento**:
   - Total de solicitudes por segundo
   - Tiempos de respuesta a lo largo del tiempo
   - Número de usuarios activos
3. **Distribución de tiempos de respuesta**: Histograma de latencias
4. **Códigos de respuesta**: Distribución de códigos HTTP
5. **Errores**: Lista detallada de fallos encontrados

### Archivos CSV

Se generan tres archivos CSV:

1. **{nombre}_stats.csv**: Estadísticas por endpoint
   - Nombre de la solicitud
   - Método HTTP
   - Número de solicitudes
   - Fallos
   - Tiempos de respuesta (promedio, mínimo, máximo, percentiles)

2. **{nombre}_stats_history.csv**: Evolución temporal de métricas
   - Timestamp
   - Usuarios activos
   - Solicitudes por segundo
   - Fallos por segundo

3. **{nombre}_failures.csv**: Detalles de errores
   - Timestamp
   - Método
   - Endpoint
   - Error
   - Cantidad de ocurrencias

## Métricas Clave de Rendimiento

### Tiempos de Respuesta Aceptables

Los siguientes umbrales se consideran aceptables para la aplicación:

- **Operaciones de lectura simples**: < 200ms (percentil 95)
- **Operaciones de lectura complejas**: < 500ms (percentil 95)
- **Operaciones de escritura**: < 1000ms (percentil 95)
- **Flujos completos**: < 2000ms (percentil 95)

### Tasa de Error Aceptable

- **Objetivo**: < 1% de tasa de error
- **Aceptable**: < 5% de tasa de error
- **Crítico**: > 5% de tasa de error (requiere investigación)

### Throughput Esperado

Capacidad mínima esperada del sistema:

- **Operaciones de lectura**: > 100 req/s
- **Operaciones de escritura**: > 20 req/s
- **Flujos completos**: > 10 flujos/s

### Análisis de Percentiles

Los percentiles son más representativos que promedios:

- **P50 (Mediana)**: 50% de las solicitudes están por debajo de este valor
- **P95**: 95% de las solicitudes están por debajo (objetivo principal)
- **P99**: 99% de las solicitudes están por debajo (casos extremos)

## Identificación de Problemas

### Problemas Comunes y Diagnóstico

#### Alta Tasa de Error

**Síntoma**: > 5% de solicitudes fallan

**Posibles causas:**
- Servicios caídos o no disponibles
- Timeout en comunicación entre microservicios
- Errores de validación de datos
- Problemas de conexión a base de datos

**Acciones:**
1. Revisar logs de los microservicios
2. Verificar estado de servicios (Eureka dashboard)
3. Revisar archivo de failures.csv para identificar patrones

#### Tiempos de Respuesta Elevados

**Síntoma**: Tiempos de respuesta > umbrales definidos

**Posibles causas:**
- Consultas ineficientes a base de datos
- Carga excesiva en CPU o memoria
- Latencia de red entre servicios
- Falta de índices en base de datos

**Acciones:**
1. Analizar logs de aplicación para queries lentas
2. Monitorear uso de recursos (CPU, RAM, disco)
3. Revisar métricas de base de datos
4. Identificar endpoints específicos con problemas

#### Degradación Progresiva

**Síntoma**: Tiempos de respuesta aumentan con el tiempo

**Posibles causas:**
- Fuga de memoria
- Acumulación de conexiones
- Llenado de pools de conexiones
- Problemas de garbage collection

**Acciones:**
1. Monitorear uso de memoria de JVM
2. Revisar configuración de pools de conexiones
3. Analizar logs de garbage collection
4. Verificar que conexiones se cierren correctamente

#### Fallo Bajo Picos

**Síntoma**: Sistema falla o degrada severamente en spike tests

**Posibles causas:**
- Falta de circuit breakers
- Límites de thread pool insuficientes
- Auto-escalado no configurado
- Falta de rate limiting

**Acciones:**
1. Implementar circuit breakers (Resilience4j)
2. Ajustar configuración de thread pools
3. Configurar auto-escalado horizontal
4. Implementar rate limiting en API Gateway

## Mejores Prácticas

### Antes de Ejecutar Pruebas

1. **Verificar estado del sistema**: Todos los servicios deben estar funcionando correctamente
2. **Limpiar datos de prueba anteriores**: Evitar interferencias con datos residuales
3. **Establecer línea base**: Ejecutar una prueba inicial con carga mínima
4. **Documentar configuración**: Registrar versión de código, configuración de infraestructura

### Durante la Ejecución

1. **Monitorear recursos**: Observar uso de CPU, memoria, disco, red
2. **Revisar logs**: Mantener logs de aplicación abiertos para identificar errores
3. **Observar métricas**: Usar herramientas como Prometheus/Grafana si están disponibles
4. **No modificar sistema**: Evitar cambios durante la ejecución de pruebas

### Después de las Pruebas

1. **Analizar resultados**: Revisar reportes HTML y CSV completos
2. **Identificar regresiones**: Comparar con resultados de pruebas anteriores
3. **Documentar hallazgos**: Registrar problemas encontrados
4. **Planificar optimizaciones**: Priorizar mejoras basadas en impacto

### Diseño de Pruebas

1. **Reflejar uso real**: Los pesos de tareas deben aproximar patrones de producción
2. **Incluir tiempos de espera**: Los usuarios reales no hacen solicitudes continuas
3. **Probar flujos completos**: Incluir flujos críticos de negocio
4. **Validar respuestas**: Usar `catch_response` para verificar contenido
5. **Distribuir carga**: No concentrar todas las solicitudes en un endpoint

### Mantenimiento de Pruebas

1. **Actualizar con código**: Mantener pruebas sincronizadas con cambios en API
2. **Revisar datos de prueba**: Asegurar que IDs utilizados existen en base de datos
3. **Ajustar parámetros**: Modificar usuarios/duración según capacidad actual
4. **Versionar resultados**: Mantener histórico de resultados para análisis de tendencias

## Integración con CI/CD

### Ejecución en Pipeline

Las pruebas de carga pueden integrarse en pipelines de CI/CD para validación automática:

```yaml
# Ejemplo para GitHub Actions
- name: Run Load Tests
  run: |
    cd locust-tests
    source venv/bin/activate
    ./run_load_test.sh
  
- name: Upload Test Reports
  uses: actions/upload-artifact@v2
  with:
    name: locust-reports
    path: locust-tests/reports/
```

### Criterios de Aceptación

Definir umbrales que deben cumplirse para aprobar el pipeline:

- Tasa de error < 1%
- P95 de tiempo de respuesta < 500ms
- Throughput > 100 req/s
- Sin errores críticos (5xx)

## Análisis Avanzado

### Identificar Servicios Lentos

Revisar el reporte CSV para identificar endpoints con mayor latencia:

```bash
# Ordenar por tiempo promedio (columna 8)
sort -t',' -k8 -n reports/load-test-*_stats.csv | tail -10
```

### Comparar Resultados

Comparar resultados de diferentes ejecuciones:

```bash
# Diferencia de tiempos promedio entre dos ejecuciones
diff <(awk -F',' '{print $1,$8}' reports/test1_stats.csv) \
     <(awk -F',' '{print $1,$8}' reports/test2_stats.csv)
```

### Calcular SLA

Verificar cumplimiento de Service Level Agreement:

```python
# Script para calcular % de requests bajo umbral
import csv

threshold_ms = 500
total = 0
within_threshold = 0

with open('reports/load-test_stats.csv') as f:
    reader = csv.DictReader(f)
    for row in reader:
        total += int(row['Request Count'])
        if float(row['Average Response Time']) < threshold_ms:
            within_threshold += int(row['Request Count'])

sla_compliance = (within_threshold / total) * 100
print(f"SLA Compliance: {sla_compliance:.2f}%")
```

## Solución de Problemas

### Locust No Inicia

**Problema**: Error al ejecutar `locust -f locustfile.py`

**Solución:**
```bash
# Verificar instalación
pip list | grep locust

# Reinstalar si es necesario
pip install --upgrade locust

# Verificar sintaxis de Python
python -m py_compile locustfile.py
```

### Error de Conexión

**Problema**: "Connection refused" o "Connection timeout"

**Solución:**
1. Verificar que API Gateway está activo: `curl http://localhost:8900/actuator/health`
2. Verificar que servicios están registrados en Eureka
3. Revisar configuración de host en test_config.py

### Errores 404 en Masa

**Problema**: Alto porcentaje de errores 404

**Solución:**
1. Verificar que los IDs en test_config.py existen en base de datos
2. Revisar rutas de endpoints en archivos de tareas
3. Confirmar que API Gateway está enrutando correctamente

### Uso Excesivo de Memoria

**Problema**: Locust consume demasiada memoria

**Solución:**
1. Reducir número de usuarios concurrentes
2. Usar FastHttpUser en lugar de HttpUser (ya implementado)
3. Ejecutar en modo distribuido con múltiples workers

## Recursos Adicionales

### Documentación Oficial

- **Locust**: https://docs.locust.io/
- **Python Requests**: https://requests.readthedocs.io/
- **Performance Testing**: https://www.perfmatrix.com/

### Herramientas Complementarias

- **Prometheus**: Métricas de sistema en tiempo real
- **Grafana**: Visualización de métricas
- **JMeter**: Alternativa para pruebas de carga
- **k6**: Herramienta moderna de pruebas de carga

## Glosario

- **RPS (Requests Per Second)**: Solicitudes por segundo
- **P95/P99**: Percentil 95/99 de tiempos de respuesta
- **Spawn Rate**: Tasa de creación de usuarios virtuales
- **Throughput**: Cantidad de transacciones procesadas por unidad de tiempo
- **Latency**: Tiempo de respuesta de una solicitud
- **Concurrent Users**: Usuarios activos simultáneamente
- **Ramp-up**: Período de incremento gradual de usuarios
- **Think Time**: Tiempo de espera entre solicitudes de un usuario

## Recomendaciones Finales

1. Ejecutar pruebas de carga regularmente, no solo antes de releases
2. Establecer líneas base de rendimiento para detectar regresiones
3. Documentar resultados y decisiones basadas en ellos
4. Ajustar parámetros de prueba según crecimiento de la aplicación
5. Mantener balance entre cobertura de pruebas y recursos disponibles
6. Complementar con monitoreo continuo en producción
7. Simular escenarios reales basados en datos de producción
8. Incluir pruebas de resistencia (endurance tests) para validar estabilidad a largo plazo
9. Coordinar pruebas de carga con equipo de infraestructura
10. Automatizar generación y análisis de reportes cuando sea posible
