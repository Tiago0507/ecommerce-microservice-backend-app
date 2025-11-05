# Documentación de Pipeline de Stage (Stage Pipeline)

## Descripción General

El pipeline de stage automatiza el proceso completo de integración continua y despliegue continuo (CI/CD) para el entorno de staging. Este pipeline ejecuta construcción, pruebas unitarias, pruebas de integración, construcción de imágenes Docker, publicación en Docker Hub, despliegue en contenedores y pruebas de rendimiento con Locust. Representa un flujo de validación completo antes de desplegar a producción.

## Archivo de Configuración

**Ubicación**: `.github/workflows/stage-pipeline.yml`

**Nombre del Workflow**: "Stage Pipeline - Build, Test, and Deploy"

## Triggers de Ejecución

El pipeline se activa automáticamente en los siguientes eventos:

### Push a Ramas Específicas

```yaml
on:
  push:
    branches:
      - stage
      - main
      - feature/stage-pipeline
```

El pipeline se ejecuta cuando se realiza un push directo a:
- `stage`: Rama de staging
- `main`: Rama principal de producción
- `feature/stage-pipeline`: Rama de desarrollo del pipeline

## Variables de Entorno Globales

El pipeline define las siguientes variables de entorno:

```yaml
env:
  JAVA_VERSION: '17'
  MAVEN_OPTS: '-Xmx1024m -XX:MaxMetaspaceSize=512m'
  DOCKERHUB_USER: santiagovg
```

- **JAVA_VERSION**: Versión de Java utilizada (JDK 17)
- **MAVEN_OPTS**: Configuración de memoria para Maven
  - `-Xmx1024m`: Heap máximo de 1 GB
  - `-XX:MaxMetaspaceSize=512m`: Metaspace máximo de 512 MB
- **DOCKERHUB_USER**: Usuario de Docker Hub para publicación de imágenes

## Arquitectura del Pipeline

El pipeline se compone de 3 jobs principales que se ejecutan secuencialmente:

```
build-and-test
      ↓
build-and-push (matriz paralela)
      ↓
deploy-and-locust-test
```

## Job 1: Build and Test

**Nombre**: `build-and-test`
**Propósito**: Compilar y ejecutar todas las pruebas (unitarias e integración)
**Runner**: `ubuntu-latest`
**Timeout**: 25 minutos

### Pasos del Job

#### 1. Checkout del Código

```yaml
- name: Checkout code
  uses: actions/checkout@v4
```

Descarga el código fuente completo del repositorio en el runner de GitHub Actions.

#### 2. Configuración de JDK 17

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: ${{ env.JAVA_VERSION }}
    distribution: 'temurin'
    cache: 'maven'
```

Configura el entorno Java con:
- **Versión**: JDK 17 (Eclipse Temurin)
- **Cache**: Habilita caché de dependencias Maven para acelerar builds

#### 3. Build y Pruebas con Maven

```yaml
- name: Build and Test with Maven
  run: |
    echo "Building, running unit tests, and integration tests..."
    mvn -T 2C clean verify -B -ntp
```

Ejecuta el ciclo completo de construcción y pruebas con:

- **-T 2C**: Construcción paralela usando 2 threads por núcleo de CPU
- **clean**: Limpia artefactos anteriores
- **verify**: Ejecuta todas las fases hasta verificación, incluyendo:
  - Compilación del código fuente
  - Ejecución de pruebas unitarias (fase `test`)
  - Empaquetado de artefactos
  - Ejecución de pruebas de integración (fase `integration-test`)
  - Verificación de resultados
- **-B**: Modo batch (no interactivo)
- **-ntp**: No muestra barra de progreso de transferencias

**Diferencia clave con Dev Pipeline**: Este pipeline ejecuta todas las pruebas, mientras que el pipeline de desarrollo las omite para mayor velocidad.

#### 4. Subida de Artefactos

```yaml
- name: Upload build artifacts (JARs)
  uses: actions/upload-artifact@v4
  with:
    name: microservices-jars
    path: |
      api-gateway/target/*.jar
      user-service/target/*.jar
      product-service/target/*.jar
      order-service/target/*.jar
      payment-service/target/*.jar
      shipping-service/target/*.jar
      favourite-service/target/*.jar
    retention-days: 7
```

Guarda todos los archivos JAR generados como artefactos con retención de 7 días.

## Job 2: Build and Push

**Nombre**: `build-and-push`
**Propósito**: Construir imágenes Docker y publicarlas en Docker Hub
**Dependencia**: Requiere que `build-and-test` se complete exitosamente
**Runner**: `ubuntu-latest`
**Timeout**: 20 minutos

### Estrategia de Matriz

Utiliza estrategia de matriz para construcción paralela:

```yaml
strategy:
  matrix:
    service:
      - api-gateway
      - user-service
      - product-service
      - order-service
      - payment-service
      - shipping-service
      - favourite-service
```

Crea 7 ejecuciones paralelas, una por cada microservicio.

### Pasos del Job

#### 1. Checkout del Código

Descarga el código fuente del repositorio.

#### 2. Descarga de Artefactos

```yaml
- name: Download build artifacts
  uses: actions/download-artifact@v4
  with:
    name: microservices-jars
    path: .
```

Descarga los archivos JAR del job anterior.

#### 3. Configuración de Docker Buildx

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3
```

Configura Docker Buildx para construcción avanzada de imágenes.

#### 4. Login a Docker Hub

```yaml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

Autentica con Docker Hub usando credenciales almacenadas en GitHub Secrets:
- **DOCKERHUB_USERNAME**: Nombre de usuario de Docker Hub
- **DOCKERHUB_TOKEN**: Token de acceso personal (PAT) de Docker Hub

**Configuración requerida**: Estos secrets deben estar configurados en GitHub Settings → Secrets and variables → Actions.

#### 5. Construcción y Publicación de Imagen

```yaml
- name: Build and Push Docker image
  id: build-push
  uses: docker/build-push-action@v5
  with:
    context: ./${{ matrix.service }}
    file: ./${{ matrix.service }}/Dockerfile
    push: true
    tags: ${{ env.DOCKERHUB_USER }}/${{ matrix.service }}-ecommerce-boot:stage-${{ github.sha }}
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

Construye y publica la imagen Docker con:
- **context**: Directorio del microservicio
- **file**: Dockerfile específico
- **push**: true (publica automáticamente a Docker Hub)
- **tags**: Etiqueta con formato `santiagovg/[servicio]-ecommerce-boot:stage-[commit-sha]`
- **cache**: Utiliza GitHub Actions cache para acelerar builds

**Ejemplo de tag generado**: `santiagovg/api-gateway-ecommerce-boot:stage-a1b2c3d4`

## Job 3: Deploy and Locust Test

**Nombre**: `deploy-and-locust-test`
**Propósito**: Desplegar microservicios y ejecutar pruebas de rendimiento
**Dependencia**: Requiere que `build-and-push` se complete exitosamente
**Runner**: `ubuntu-latest`
**Timeout**: 15 minutos

### Pasos del Job

#### 1. Checkout del Código

Descarga el código fuente para acceder a archivos de configuración.

#### 2. Login a Docker Hub

```yaml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```

Autentica con Docker Hub para descargar las imágenes recién publicadas.

#### 3. Configuración de Variable de Tag

```yaml
- name: Set Image Tag Env Var
  run: |
    echo "IMAGE_TAG=stage-${{ github.sha }}" >> $GITHUB_ENV
    echo "TAG set to: $IMAGE_TAG"
```

Crea variable de entorno `IMAGE_TAG` con el valor `stage-[commit-sha]` para referenciar las imágenes correctas.

#### 4. Actualización de compose.yml

```yaml
- name: Update compose.yml with new Image Tags
  run: |
    echo "Replacing image tags in compose.yml..."
    sed -i "s|image: tiago0507/\([a-z-]*\)-ecommerce-boot:local|image: ${{ env.DOCKERHUB_USER }}/\1-ecommerce-boot:${{ env.IMAGE_TAG }}|g" compose.yml
    
    echo "--- Updated compose.yml ---"
    cat compose.yml | grep "image:"
    echo "---------------------------"
```

Reemplaza dinámicamente las referencias de imagen en `compose.yml`:
- **De**: `tiago0507/[servicio]-ecommerce-boot:local`
- **A**: `santiagovg/[servicio]-ecommerce-boot:stage-[commit-sha]`

Usa `sed` para reemplazo de texto y muestra las líneas modificadas para verificación.

#### 5. Creación de Red Docker

```yaml
- name: Create Docker network
  run: docker network create microservices_network || true
```

Crea red Docker personalizada para comunicación entre contenedores.

#### 6. Inicio de Servicios Core

```yaml
- name: Start core services
  run: |
    echo "Starting core infrastructure (Eureka, Config, Zipkin)..."
    docker compose -f core.yml up -d
    
    echo "Waiting for Eureka to be ready..."
    timeout 120 bash -c 'until curl -sf http://localhost:8761/actuator/health; do sleep 5; done' || {
      echo "Eureka failed to start"
      docker compose -f core.yml logs
      exit 1
    }
    echo "Core services ready"
```

Inicia servicios de infraestructura:
- **Eureka Server**: Service Discovery (puerto 8761)
- **Config Server**: Configuración centralizada
- **Zipkin**: Trazabilidad distribuida

Incluye verificación de salud activa con timeout de 120 segundos.

#### 7. Inicio de Microservicios

```yaml
- name: Start microservices
  run: |
    echo "Starting microservices (Pulling from Docker Hub)..."
    docker compose -f compose.yml up -d
    
    echo "Waiting 60s for all services to start and register in Eureka..."
    sleep 60
```

Inicia todos los microservicios descargando las imágenes desde Docker Hub y espera 60 segundos para:
- Inicialización completa de servicios
- Registro en Eureka
- Establecimiento de conexiones entre servicios

#### 8. Verificación de Despliegue

```yaml
- name: Verify deployment
  run: |
    echo "Checking container status..."
    docker compose -f compose.yml ps
```

Muestra el estado de todos los contenedores desplegados.

#### 9. Ejecución de Pruebas de Locust

```yaml
- name: Run Locust Performance Tests
  run: |
    echo "Setting up Python virtual environment..."
    python3 -m venv locust-venv
    source locust-venv/bin/activate
    
    echo "Installing Locust..."
    pip install --upgrade pip
    pip install -r locust-tests/requirements.txt
    
    echo "Running Locust tests..."
    cd locust-tests
    mkdir -p reports
    
    locust -f locustfile.py \
      --host=http://localhost:8080 \
      --users 100 \
      --spawn-rate 10 \
      --run-time 3m \
      --headless \
      --html reports/report.html \
      --csv reports/stats \
      --check-fail-ratio 0.05 \
      --check-avg-response-time 2000
    
    echo "Locust tests completed successfully."
```

Ejecuta pruebas de rendimiento con Locust:

**Configuración del entorno:**
- Crea entorno virtual de Python
- Instala Locust y dependencias

**Parámetros de prueba:**
- **--host**: URL del API Gateway (http://localhost:8080)
- **--users**: 100 usuarios concurrentes simulados
- **--spawn-rate**: 10 usuarios/segundo de tasa de generación
- **--run-time**: 3 minutos de duración
- **--headless**: Sin interfaz web (modo automático)
- **--html**: Genera reporte HTML
- **--csv**: Genera reportes CSV
- **--check-fail-ratio**: Falla si tasa de error > 5%
- **--check-avg-response-time**: Falla si tiempo promedio > 2000ms

**Criterios de éxito:**
- Tasa de error menor a 5%
- Tiempo de respuesta promedio menor a 2 segundos

Si algún criterio no se cumple, el paso falla y detiene el pipeline.

#### 10. Subida de Reportes de Locust

```yaml
- name: Upload Locust Reports
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: locust-reports
    path: locust-tests/reports/
    retention-days: 7
```

Guarda los reportes de Locust como artefactos:
- Reporte HTML visual
- Archivos CSV con estadísticas
- Retención de 7 días
- Se ejecuta siempre (incluso si las pruebas fallan)

#### 11. Subida de Logs en Caso de Fallo

```yaml
- name: Upload logs on failure
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: stage-deployment-logs
    path: |
      *.logs
    retention-days: 3
```

Guarda logs solo si el pipeline falla, con retención de 3 días.

#### 12. Limpieza

```yaml
- name: Cleanup
  if: always()
  run: |
    echo "Cleaning up Docker environment..."
    docker compose -f compose.yml logs --tail=100 || true
    docker compose -f compose.yml down -v || true
    docker compose -f core.yml down -v || true
    docker network rm microservices_network || true
```

Limpia el entorno Docker (siempre se ejecuta):
- Muestra últimas 100 líneas de logs
- Detiene y elimina contenedores de microservicios
- Detiene y elimina contenedores de servicios core
- Elimina red Docker
- Elimina volúmenes asociados

## Microservicios Incluidos

El pipeline procesa los siguientes microservicios:

1. **api-gateway**: Gateway de API para enrutamiento
2. **user-service**: Gestión de usuarios y autenticación
3. **product-service**: Gestión de catálogo de productos
4. **order-service**: Gestión de pedidos y carritos
5. **payment-service**: Procesamiento de pagos
6. **shipping-service**: Gestión de envíos
7. **favourite-service**: Gestión de productos favoritos

## Tiempos de Ejecución

**Timeouts configurados:**
- Build and Test: 25 minutos
- Build and Push: 20 minutos por servicio
- Deploy and Locust Test: 15 minutos

**Tiempo total estimado:** 30-45 minutos (dependiendo de cache y paralelización)

## Artefactos Generados

El pipeline genera los siguientes artefactos:

1. **microservices-jars**: Archivos JAR compilados (retención: 7 días)
2. **locust-reports**: Reportes de pruebas de rendimiento (retención: 7 días)
   - report.html: Reporte visual interactivo
   - stats.csv: Estadísticas por endpoint
   - stats_history.csv: Evolución temporal
   - stats_failures.csv: Detalle de errores
3. **stage-deployment-logs**: Logs en caso de fallo (retención: 3 días)

## Imágenes Docker Publicadas

El pipeline publica las siguientes imágenes en Docker Hub:

**Formato de etiqueta**: `santiagovg/[servicio]-ecommerce-boot:stage-[commit-sha]`

**Ejemplo**:
- `santiagovg/api-gateway-ecommerce-boot:stage-a1b2c3d4`
- `santiagovg/user-service-ecommerce-boot:stage-a1b2c3d4`
- `santiagovg/product-service-ecommerce-boot:stage-a1b2c3d4`

Las imágenes permanecen en Docker Hub hasta que se eliminen manualmente.

## Configuración de Secrets

El pipeline requiere los siguientes secrets configurados en GitHub:

### DOCKERHUB_USERNAME

**Tipo**: String
**Descripción**: Nombre de usuario de Docker Hub
**Ejemplo**: `santiagovg`

### DOCKERHUB_TOKEN

**Tipo**: Token
**Descripción**: Token de acceso personal de Docker Hub

**Cómo obtenerlo:**
1. Iniciar sesión en Docker Hub
2. Ir a Account Settings → Security
3. Crear New Access Token
4. Copiar el token generado

**Cómo configurarlo:**
1. Ir a GitHub → Settings → Secrets and variables → Actions
2. Hacer clic en "New repository secret"
3. Nombre: `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`
4. Pegar el valor correspondiente
5. Guardar

## Optimizaciones Implementadas

### Construcción Paralela

- **Maven Multi-threading**: `-T 2C` usa 2 threads por CPU core
- **Estrategia de Matriz**: 7 imágenes Docker construidas en paralelo

### Sistema de Caché

- **Maven Dependencies**: Cache de dependencias entre ejecuciones
- **Docker Layers**: Cache de capas Docker en GitHub Actions

### Ejecución Eficiente

- **Reutilización de Artefactos**: Los JARs se construyen una vez y se reutilizan
- **Docker Buildx**: Construcción optimizada de imágenes

## Criterios de Calidad

El pipeline implementa múltiples comprobaciones de calidad:

### Pruebas

- **Pruebas Unitarias**: Validan lógica de negocio aislada
- **Pruebas de Integración**: Validan interacción entre componentes
- **Pruebas de Rendimiento**: Validan comportamiento bajo carga

### Umbrales de Rendimiento

- **Tasa de error máxima**: 5%
- **Tiempo de respuesta promedio máximo**: 2000ms
- **Usuarios concurrentes**: 100 usuarios simulados
- **Duración de prueba**: 3 minutos

Si algún umbral no se cumple, el pipeline falla.

## Manejo de Errores

### Estrategias Implementadas

1. **Timeouts**: Cada job tiene límite de tiempo máximo
2. **Verificación de Salud**: Comprobación activa de servicios core
3. **Criterios de Locust**: Fallos automáticos si no se cumplen umbrales
4. **Captura de Logs**: Logs guardados automáticamente en fallos
5. **Limpieza Garantizada**: Cleanup siempre se ejecuta

### Puntos de Fallo Comunes

1. **Fallo en Tests**: Si pruebas unitarias o integración fallan
2. **Fallo en Docker Build**: Si construcción de imagen falla
3. **Fallo en Push**: Si autenticación o subida a Docker Hub falla
4. **Fallo en Despliegue**: Si servicios no inician correctamente
5. **Fallo en Locust**: Si pruebas de rendimiento no cumplen umbrales

## Monitoreo y Depuración

### Ver Ejecución en Vivo

1. Ir a pestaña "Actions" en GitHub
2. Seleccionar "Stage Pipeline - Build, Test, and Deploy"
3. Hacer clic en la ejecución en curso
4. Expandir jobs y pasos para ver logs en tiempo real

### Revisar Reportes de Locust

1. Ir a la página de ejecución del workflow
2. Scroll hasta "Artifacts"
3. Descargar "locust-reports"
4. Abrir `report.html` en navegador

El reporte incluye:
- Gráficas de rendimiento
- Estadísticas por endpoint
- Distribución de tiempos de respuesta
- Lista de errores

### Depurar Fallos

**Si fallan las pruebas:**
1. Revisar logs del paso "Build and Test with Maven"
2. Identificar prueba específica que falló
3. Ejecutar localmente: `mvn test -Dtest=NombrePrueba`

**Si falla construcción de Docker:**
1. Revisar logs del paso "Build and Push Docker image"
2. Verificar Dockerfile del servicio afectado
3. Probar localmente: `docker build -t test .`

**Si falla Locust:**
1. Descargar reporte de locust-reports
2. Revisar archivo stats_failures.csv
3. Identificar endpoints con alta tasa de error
4. Revisar logs de microservicios afectados

## Mejores Prácticas

### Antes de Ejecutar

1. Ejecutar pruebas localmente: `mvn clean verify`
2. Verificar que secrets de Docker Hub estén configurados
3. Revisar que todas las pruebas pasen
4. Hacer commit con mensaje descriptivo

### Durante la Ejecución

1. Monitorear progreso en GitHub Actions
2. No hacer push adicionales mientras se ejecuta
3. Revisar logs si algún paso toma demasiado tiempo

### Después de la Ejecución

1. Revisar reporte de Locust aunque el pipeline pase
2. Comparar métricas con ejecuciones anteriores
3. Verificar imágenes en Docker Hub
4. Documentar cualquier problema encontrado

## Comparación con Dev Pipeline

| Aspecto | Dev Pipeline | Stage Pipeline |
|---------|--------------|----------------|
| Pruebas | ❌ Omitidas | ✅ Completas (unit + integration) |
| Pruebas de Carga | ❌ No incluidas | ✅ Locust tests |
| Docker Hub | ❌ Solo local | ✅ Publicación automática |
| Tiempo de Ejecución | ~15-20 min | ~30-45 min |
| Validación | Básica | Exhaustiva |
| Uso | Desarrollo rápido | Pre-producción |
| Artefactos | Temporal | Persistente |
| Etiquetas Docker | `local` | `stage-[commit-sha]` |

## Uso Recomendado

### Cuándo Usar Stage Pipeline

- Antes de hacer merge a rama `main`
- Para validación completa de features
- Antes de releases importantes
- Para validar rendimiento
- Para generar imágenes de staging

### Cuándo Usar Dev Pipeline

- Durante desarrollo activo
- Para validación rápida de compilación
- Para iteraciones rápidas
- Cuando no se necesitan pruebas completas

## Integración con Flujo de Trabajo

### Flujo Típico

1. **Desarrollo**: Trabajar en rama feature con Dev Pipeline
2. **Pull Request**: Crear PR a `stage` (ejecuta Stage Pipeline)
3. **Revisión**: Revisar código y resultados de pipeline
4. **Merge a Stage**: Si todo pasa, merge a `stage`
5. **Validación Final**: Stage Pipeline se ejecuta nuevamente
6. **Merge a Main**: Si staging es exitoso, merge a `main`

### Proceso de Release

1. Merge de `stage` a `main`
2. Stage Pipeline se ejecuta en `main`
3. Imágenes etiquetadas con commit de `main`
4. Usar imágenes para despliegue en producción

## Requisitos del Sistema

### GitHub Actions

- Runners disponibles
- Espacio de almacenamiento para artefactos
- Límites de minutos no excedidos

### Docker Hub

- Cuenta activa
- Token de acceso configurado
- Espacio disponible para imágenes

### Repositorio

- Código fuente completo
- Dockerfiles en cada microservicio
- Archivos `core.yml` y `compose.yml`
- Directorio `locust-tests` con pruebas

## Limitaciones

El pipeline tiene las siguientes limitaciones:

- Ejecuta en entorno efímero (se destruye al terminar)
- No despliega a infraestructura real de staging
- Limitado por recursos de GitHub Actions runners
- Tiempo máximo de ejecución limitado por timeouts
- No incluye pruebas end-to-end completas

## Solución de Problemas Comunes

### Error: "DOCKERHUB_USERNAME secret not found"

**Causa**: Secrets no configurados en GitHub

**Solución:**
1. Ir a Settings → Secrets and variables → Actions
2. Agregar `DOCKERHUB_USERNAME` y `DOCKERHUB_TOKEN`
3. Re-ejecutar el pipeline

### Error: "Locust tests failed: fail_ratio > 0.05"

**Causa**: Más de 5% de solicitudes fallaron

**Solución:**
1. Descargar reporte de Locust
2. Revisar stats_failures.csv
3. Identificar servicios con problemas
4. Revisar logs de esos servicios
5. Corregir errores y re-ejecutar

### Error: "Eureka failed to start"

**Causa**: Eureka no inició en 120 segundos

**Solución:**
1. Revisar logs de core services
2. Verificar configuración en core.yml
3. Aumentar timeout si es necesario
4. Verificar recursos del runner

### Error: "Docker image push failed"

**Causa**: Fallo al subir imagen a Docker Hub

**Solución:**
1. Verificar credenciales de Docker Hub
2. Verificar espacio disponible en Docker Hub
3. Revisar conectividad de red
4. Verificar límites de rate de Docker Hub

## Métricas y KPIs

El pipeline recopila las siguientes métricas:

### Métricas de Build

- Tiempo de compilación
- Número de pruebas ejecutadas
- Cobertura de código (si está configurado)

### Métricas de Rendimiento

- Solicitudes por segundo
- Tiempo de respuesta promedio
- Percentiles P50, P95, P99
- Tasa de error
- Distribución de códigos HTTP

### Métricas de Despliegue

- Tiempo total de pipeline
- Tiempo de construcción de imágenes
- Tiempo de despliegue
- Número de reintentos

## Recomendaciones Finales

1. **Ejecutar regularmente**: No esperar a release para usar este pipeline
2. **Monitorear tendencias**: Comparar métricas de Locust entre ejecuciones
3. **Mantener umbrales**: Ajustar criterios según capacidad actual
4. **Documentar cambios**: Registrar modificaciones al pipeline
5. **Revisar logs**: Incluso en ejecuciones exitosas
6. **Optimizar pruebas**: Mantener pruebas rápidas pero completas
7. **Gestionar imágenes**: Limpiar imágenes antiguas de Docker Hub
8. **Actualizar dependencias**: Mantener actions actualizadas
9. **Probar localmente**: Validar cambios antes de push
10. **Automatizar análisis**: Considerar herramientas de análisis de reportes

## Próximos Pasos

Posibles mejoras futuras para el pipeline:

1. Integrar análisis de código estático (SonarQube)
2. Agregar escaneo de vulnerabilidades de imágenes
3. Implementar despliegue a Kubernetes
4. Agregar pruebas de seguridad automatizadas
5. Integrar notificaciones (Slack, email)
6. Agregar análisis de cobertura de código
7. Implementar promoción automática a producción
8. Agregar smoke tests post-despliegue
9. Integrar métricas en dashboard central
10. Implementar rollback automático en fallos
