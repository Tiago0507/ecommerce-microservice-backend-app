# Documentación de Pipeline de Desarrollo (Dev Pipeline)

## Descripción General

El pipeline de desarrollo automatiza el proceso de construcción, empaquetado, creación de imágenes Docker y despliegue de todos los microservicios en un entorno de desarrollo. Este pipeline se ejecuta automáticamente en GitHub Actions cuando se realizan cambios en las ramas de desarrollo, permitiendo validar rápidamente los cambios sin ejecutar pruebas exhaustivas.

## Archivo de Configuración

**Ubicación**: `.github/workflows/dev-pipeline.yml`

**Nombre del Workflow**: "Dev Pipeline - Build and Deploy Microservices"

## Triggers de Ejecución

El pipeline se activa automáticamente en los siguientes eventos:

### Push a Ramas Específicas

```yaml
on:
  push:
    branches: 
      - 'develop'
      - 'feature/jenkins-pipelines'
```

El pipeline se ejecuta cuando se realiza un push directo a:
- `develop`: Rama principal de desarrollo
- `feature/jenkins-pipelines`: Rama de desarrollo de pipelines

### Pull Requests

```yaml
on:
  pull_request:
    branches: 
      - 'develop'
```

El pipeline también se ejecuta cuando se crea o actualiza un pull request hacia la rama `develop`.

## Variables de Entorno Globales

El pipeline define las siguientes variables de entorno disponibles para todos los jobs:

```yaml
env:
  JAVA_VERSION: '17'
  MAVEN_OPTS: '-Xmx1024m -XX:MaxMetaspaceSize=512m'
```

- **JAVA_VERSION**: Versión de Java utilizada (JDK 17)
- **MAVEN_OPTS**: Opciones de configuración de memoria para Maven
  - `-Xmx1024m`: Heap máximo de 1 GB
  - `-XX:MaxMetaspaceSize=512m`: Metaspace máximo de 512 MB

## Arquitectura del Pipeline

El pipeline se compone de 4 jobs principales que se ejecutan secuencialmente:

```
build-and-test
      ↓
build-docker-images (matriz paralela)
      ↓
deploy-dev
      ↓
summary
```

## Job 1: Build and Test

**Nombre**: `build-and-test`
**Propósito**: Compilar todos los microservicios con Maven
**Runner**: `ubuntu-latest`
**Timeout**: 20 minutos

### Pasos del Job

#### 1. Checkout del Código

```yaml
- name: Checkout code
  uses: actions/checkout@v4
```

Descarga el código fuente del repositorio en el runner de GitHub Actions.

#### 2. Configuración de JDK 17

```yaml
- name: Set up JDK 17
  uses: actions/setup-java@v4
  with:
    java-version: ${{ env.JAVA_VERSION }}
    distribution: 'temurin'
    cache: 'maven'
```

Configura el entorno Java utilizando:
- **Versión**: JDK 17
- **Distribución**: Eclipse Temurin (AdoptOpenJDK)
- **Cache**: Habilita caché de dependencias Maven para acelerar builds posteriores

#### 3. Build con Maven

```yaml
- name: Build all services with Maven
  run: |
    mvn -T 2C clean package -DskipTests \
      -Dmaven.test.skip=true \
      -Dmaven.javadoc.skip=true \
      -Dmaven.source.skip=true \
      -q -ntp -B
```

Compila todos los microservicios con las siguientes opciones:

- **-T 2C**: Construcción paralela usando 2 threads por núcleo de CPU
- **clean package**: Limpia y empaqueta los artefactos
- **-DskipTests**: Omite la ejecución de pruebas (solo compilación rápida)
- **-Dmaven.test.skip=true**: Omite compilación de pruebas
- **-Dmaven.javadoc.skip=true**: Omite generación de Javadoc
- **-Dmaven.source.skip=true**: Omite empaquetado de fuentes
- **-q**: Modo silencioso (menos salida en consola)
- **-ntp**: No muestra barra de progreso de transferencias
- **-B**: Modo batch (no interactivo)

#### 4. Subida de Artefactos

```yaml
- name: Upload build artifacts
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

Guarda los archivos JAR generados como artefactos de GitHub Actions con:
- **Nombre**: microservices-jars
- **Contenido**: Todos los archivos JAR de cada microservicio
- **Retención**: 7 días

## Job 2: Build Docker Images

**Nombre**: `build-docker-images`
**Propósito**: Crear imágenes Docker para cada microservicio
**Dependencia**: Requiere que `build-and-test` se complete exitosamente
**Runner**: `ubuntu-latest`
**Timeout**: 15 minutos

### Estrategia de Matriz

El job utiliza una estrategia de matriz para construir imágenes en paralelo:

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

Esto crea 7 ejecuciones paralelas, una por cada microservicio.

### Pasos del Job

#### 1. Checkout del Código

Descarga el código fuente nuevamente en el runner.

#### 2. Descarga de Artefactos

```yaml
- name: Download build artifacts
  uses: actions/download-artifact@v4
  with:
    name: microservices-jars
    path: .
```

Descarga los archivos JAR generados en el job anterior.

#### 3. Configuración de Docker Buildx

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v3
```

Configura Docker Buildx, una extensión avanzada de Docker para construcción de imágenes con:
- Soporte para múltiples plataformas
- Construcción en paralelo mejorada
- Caché avanzado

#### 4. Construcción de Imagen Docker

```yaml
- name: Build Docker image
  uses: docker/build-push-action@v5
  with:
    context: ./${{ matrix.service }}
    file: ./${{ matrix.service }}/Dockerfile
    push: false
    load: true
    tags: tiago0507/${{ matrix.service }}-ecommerce-boot:local
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

Construye la imagen Docker con:
- **context**: Directorio del microservicio específico
- **file**: Dockerfile del microservicio
- **push**: false (no sube a registry en este punto)
- **load**: true (carga la imagen en el daemon de Docker local)
- **tags**: Etiqueta la imagen como `tiago0507/[servicio]-ecommerce-boot:local`
- **cache-from/cache-to**: Utiliza GitHub Actions cache para acelerar builds

#### 5. Guardado de Imagen a TAR

```yaml
- name: Save Docker image to tar
  run: |
    docker save tiago0507/${{ matrix.service }}-ecommerce-boot:local -o ${{ matrix.service }}-image.tar
```

Exporta la imagen Docker a un archivo TAR para transferencia entre jobs.

#### 6. Subida de Imagen como Artefacto

```yaml
- name: Upload Docker image as artifact
  uses: actions/upload-artifact@v4
  with:
    name: docker-image-${{ matrix.service }}
    path: ${{ matrix.service }}-image.tar
    retention-days: 1
```

Guarda cada imagen como artefacto separado con retención de 1 día.

## Job 3: Deploy Dev

**Nombre**: `deploy-dev`
**Propósito**: Desplegar microservicios en entorno local usando Docker Compose
**Dependencia**: Requiere que `build-docker-images` se complete exitosamente
**Runner**: `ubuntu-latest`
**Timeout**: 10 minutos

### Pasos del Job

#### 1. Checkout del Código

Descarga el código fuente para acceder a archivos de Docker Compose.

#### 2. Descarga de Todas las Imágenes Docker

```yaml
- name: Download all Docker images
  uses: actions/download-artifact@v4
  with:
    pattern: docker-image-*
    merge-multiple: true
```

Descarga todos los archivos TAR de imágenes Docker usando un patrón de coincidencia.

#### 3. Carga de Imágenes Docker

```yaml
- name: Load Docker images
  run: |
    echo "Loading Docker images..."
    for img in *-image.tar; do
      echo "Loading $img..."
      docker load -i "$img"
    done
    
    echo "Verifying images loaded:"
    docker images | grep tiago0507
```

Carga todas las imágenes Docker desde archivos TAR y verifica que estén disponibles.

#### 4. Creación de Red Docker

```yaml
- name: Create Docker network
  run: |
    docker network create microservices_network || true
```

Crea una red Docker personalizada para comunicación entre contenedores. El `|| true` evita fallo si la red ya existe.

#### 5. Inicio de Servicios Core

```yaml
- name: Start core services
  run: |
    echo "Starting core infrastructure..."
    docker compose -f core.yml up -d
    
    echo "Waiting for Eureka to be ready..."
    timeout 120 bash -c 'until curl -sf http://localhost:8761/actuator/health; do sleep 5; done' || {
      echo "Eureka failed to start"
      docker compose -f core.yml logs
      exit 1
    }
    echo "✅ Core services ready"
```

Inicia los servicios de infraestructura básica:
- **Service Discovery (Eureka)**: Puerto 8761
- **Config Server**: Configuración centralizada
- **Zipkin**: Trazabilidad distribuida

Incluye verificación de salud con timeout de 120 segundos.

#### 6. Inicio de Microservicios

```yaml
- name: Start microservices
  run: |
    echo "Starting microservices..."
    docker compose -f compose.yml up -d \
      api-gateway-container \
      user-service-container \
      product-service-container \
      order-service-container \
      payment-service-container \
      shipping-service-container \
      favourite-service-container
    
    echo "Waiting for services to start..."
    sleep 30
```

Inicia todos los microservicios de la aplicación con una espera de 30 segundos para inicialización.

#### 7. Verificación de Despliegue

```yaml
- name: Verify deployment
  run: |
    echo "Checking container status..."
    docker compose -f compose.yml ps
    
    echo "Verifying services are running..."
    docker compose -f compose.yml ps | grep -E "(api-gateway|user-service|product-service|order-service|payment-service|shipping-service|favourite-service)" | grep -q "Up"
    
    if [ $? -eq 0 ]; then
      echo "✅ All microservices are running"
    else
      echo "❌ Some services failed to start"
      docker compose -f compose.yml logs --tail=50
      exit 1
    fi
```

Verifica que todos los contenedores estén en estado "Up". Si algún servicio falla, muestra logs y termina el pipeline con error.

#### 8. Subida de Logs

```yaml
- name: Upload logs
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: deployment-logs
    path: |
      *.logs
    retention-days: 3
```

Guarda logs del despliegue (se ejecuta siempre, incluso en caso de fallo).

#### 9. Limpieza

```yaml
- name: Cleanup
  if: always()
  run: |
    docker compose -f compose.yml down -v || true
    docker compose -f core.yml down -v || true
    docker network rm microservices_network || true
```

Limpia el entorno Docker eliminando:
- Contenedores de microservicios
- Contenedores de servicios core
- Red Docker creada
- Volúmenes asociados

## Job 4: Summary

**Nombre**: `summary`
**Propósito**: Generar resumen del pipeline en GitHub Actions
**Dependencia**: Requiere todos los jobs anteriores
**Condición**: Se ejecuta siempre (incluso si hay fallos)
**Runner**: `ubuntu-latest`

### Paso del Job

#### Generación de Resumen

```yaml
- name: Generate summary
  run: |
    echo "## 🎉 Dev Pipeline Completed" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "**Branch:** ${{ github.ref_name }}" >> $GITHUB_STEP_SUMMARY
    echo "**Commit:** ${{ github.sha }}" >> $GITHUB_STEP_SUMMARY
    echo "**Triggered by:** ${{ github.actor }}" >> $GITHUB_STEP_SUMMARY
    echo "" >> $GITHUB_STEP_SUMMARY
    echo "### ✅ Microservices Built:" >> $GITHUB_STEP_SUMMARY
    echo "- api-gateway" >> $GITHUB_STEP_SUMMARY
    echo "- user-service" >> $GITHUB_STEP_SUMMARY
    echo "- product-service" >> $GITHUB_STEP_SUMMARY
    echo "- order-service" >> $GITHUB_STEP_SUMMARY
    echo "- payment-service" >> $GITHUB_STEP_SUMMARY
    echo "- shipping-service" >> $GITHUB_STEP_SUMMARY
    echo "- favourite-service" >> $GITHUB_STEP_SUMMARY
```

Crea un resumen visual en la interfaz de GitHub Actions con:
- Rama ejecutada
- Hash del commit
- Usuario que activó el pipeline
- Lista de microservicios construidos

## Microservicios Incluidos

El pipeline procesa los siguientes microservicios:

1. **api-gateway**: Gateway de API para enrutamiento
2. **user-service**: Gestión de usuarios
3. **product-service**: Gestión de productos
4. **order-service**: Gestión de pedidos
5. **payment-service**: Procesamiento de pagos
6. **shipping-service**: Gestión de envíos
7. **favourite-service**: Gestión de favoritos

## Tiempos de Ejecución

**Timeouts configurados:**
- Build and Test: 20 minutos
- Build Docker Images: 15 minutos por servicio
- Deploy Dev: 10 minutos

**Tiempo total estimado:** 15-25 minutos (dependiendo de cache y paralelización)

## Artefactos Generados

El pipeline genera los siguientes artefactos:

1. **microservices-jars**: Archivos JAR compilados (retención: 7 días)
2. **docker-image-[servicio]**: Imágenes Docker en formato TAR (retención: 1 día)
3. **deployment-logs**: Logs de despliegue (retención: 3 días)

## Optimizaciones Implementadas

### Construcción Paralela

- **Maven Multi-threading**: `-T 2C` utiliza 2 threads por CPU core
- **Estrategia de Matriz**: Construcción paralela de 7 imágenes Docker simultáneamente

### Sistema de Caché

- **Maven Dependencies**: Cache de dependencias de Maven entre ejecuciones
- **Docker Layers**: Cache de capas Docker usando GitHub Actions cache

### Construcción Rápida

- **Skip Tests**: Omite ejecución de pruebas para builds rápidos
- **Skip Javadoc**: No genera documentación
- **Skip Sources**: No empaqueta código fuente

## Manejo de Errores

El pipeline implementa varios mecanismos de manejo de errores:

### Timeouts

Cada job tiene timeout definido para evitar ejecuciones colgadas.

### Verificaciones de Salud

Verificación activa de que Eureka esté disponible antes de iniciar microservicios.

### Logs en Fallos

Captura y sube logs automáticamente cuando hay fallos.

### Cleanup Garantizado

La limpieza se ejecuta siempre (`if: always()`) incluso en caso de error.

## Requisitos y Prerrequisitos

### Repositorio

- Código fuente de todos los microservicios
- Dockerfiles en cada directorio de microservicio
- Archivos `core.yml` y `compose.yml` en raíz

### Configuración de GitHub

- GitHub Actions habilitado en el repositorio
- Runners de GitHub Actions disponibles

### Dependencias del Sistema

- JDK 17
- Maven 3.x
- Docker y Docker Compose

## Ejecución Manual

Para ejecutar el pipeline manualmente desde GitHub:

1. Ir a la pestaña "Actions" del repositorio
2. Seleccionar "Dev Pipeline - Build and Deploy Microservices"
3. Hacer clic en "Run workflow"
4. Seleccionar la rama
5. Hacer clic en "Run workflow"

## Monitoreo y Depuración

### Ver Ejecución en Vivo

Acceder a la pestaña "Actions" en GitHub y seleccionar la ejecución en curso para ver logs en tiempo real.

### Revisar Logs de Fallos

Si el pipeline falla:
1. Identificar el job que falló (marcado en rojo)
2. Expandir el paso específico que causó el error
3. Revisar el output de consola
4. Descargar artefactos de logs si están disponibles

### Artefactos Descargables

Los artefactos pueden descargarse desde:
- Página de la ejecución del workflow
- Sección "Artifacts" al final de la página

## Mejores Prácticas

### Uso del Pipeline

1. **Commits frecuentes**: Activar el pipeline regularmente para detección temprana de problemas
2. **Mensajes descriptivos**: Usar mensajes de commit claros para identificar cambios
3. **Revisar logs**: Siempre revisar logs aunque el pipeline pase exitosamente

### Resolución de Problemas

1. **Build failures**: Verificar errores de compilación en logs de Maven
2. **Docker failures**: Revisar Dockerfiles y dependencias de imagen
3. **Deployment failures**: Verificar configuración de Docker Compose y puertos

### Optimización

1. **Monitorear tiempos**: Identificar jobs que toman demasiado tiempo
2. **Aprovechar cache**: Asegurar que cache de Maven y Docker funcione correctamente
3. **Paralelización**: Considerar aumentar paralelización si hay recursos disponibles

## Diferencias con Stage Pipeline

El pipeline de desarrollo se diferencia del pipeline de stage en:

- **No ejecuta pruebas**: Omite pruebas unitarias e integración para builds rápidos
- **No sube a Docker Hub**: Las imágenes se quedan locales
- **No ejecuta pruebas de carga**: No incluye Locust tests
- **Despliegue local**: Usa imágenes locales en lugar de registry
- **Retención corta**: Artefactos se eliminan más rápido

## Limitaciones

El pipeline de desarrollo tiene las siguientes limitaciones:

- No valida calidad de código con pruebas
- No verifica rendimiento bajo carga
- Despliegue es efímero (se elimina al final)
- No genera imágenes publicadas en registry
- No está diseñado para validación exhaustiva

## Recomendaciones

1. Usar este pipeline para validación rápida de compilación
2. No confiar solo en este pipeline para validación completa
3. Ejecutar Stage Pipeline antes de merges a main
4. Revisar logs de despliegue incluso en ejecuciones exitosas
5. Mantener Dockerfiles optimizados para construcción rápida
6. Monitorear uso de recursos de GitHub Actions
7. Limpiar artefactos antiguos periódicamente
8. Documentar cambios en pipeline con commits descriptivos
