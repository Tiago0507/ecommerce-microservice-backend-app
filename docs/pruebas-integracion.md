# Documentación de Pruebas de Integración

## Descripción General

Las pruebas de integración del proyecto validan el funcionamiento correcto de los componentes cuando trabajan en conjunto, utilizando el contexto completo de Spring Boot. Estas pruebas verifican la integración entre capas (servicio, repositorio) y simulan la comunicación con servicios externos mediante mocks de RestTemplate, garantizando que los diferentes componentes del sistema interactúan correctamente.

## Estructura y Ubicación

Las pruebas de integración se encuentran organizadas en la siguiente estructura dentro de cada microservicio:

```
<microservicio>/src/test/java/com/selimhorri/app/integration/
```

Cada archivo de prueba sigue la convención de nomenclatura `<NombreServicio>IntegrationTest.java`, indicando claramente que se trata de pruebas de integración.

### Microservicios con Pruebas de Integración

El proyecto cuenta con pruebas de integración en los siguientes microservicios:

- **favourite-service**: `FavouriteServiceIntegrationTest` - Integración con User Service y Product Service
- **order-service**: `CartServiceIntegrationTest` - Integración con User Service
- **payment-service**: `PaymentServiceIntegrationTest` - Integración con Order Service
- **shipping-service**: `OrderItemServiceIntegrationTest` - Integración con Product Service y Order Service

## Tecnologías y Herramientas

Las pruebas de integración utilizan las siguientes tecnologías:

- **Spring Boot Test**: Proporciona el contexto completo de la aplicación
- **JUnit 5 (Jupiter)**: Framework principal para la ejecución de pruebas
- **Mockito**: Para simular servicios externos mediante `@MockBean`
- **AssertJ**: Librería de aserciones fluidas para validaciones expresivas
- **H2 Database**: Base de datos embebida en memoria para pruebas
- **TestRestTemplate**: Cliente HTTP configurado para pruebas (cuando es necesario)
- **Spring Data JPA**: Para operaciones reales con la base de datos de prueba

## Diferencias con Pruebas Unitarias

Las pruebas de integración se distinguen de las unitarias en los siguientes aspectos:

| Aspecto | Pruebas Unitarias | Pruebas de Integración |
|---------|-------------------|------------------------|
| Contexto Spring | No se carga | Se carga completamente |
| Base de datos | Completamente simulada | Base de datos embebida real |
| Repositorios | Mocks | Instancias reales de JPA |
| Transacciones | No aplica | Transacciones reales |
| Servicios externos | Mocks con Mockito | Mocks con @MockBean |
| Velocidad | Muy rápidas | Más lentas |
| Alcance | Método individual | Integración entre capas |

## Patrón de Diseño

### Anotaciones de Clase

Las pruebas de integración utilizan las siguientes anotaciones clave:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(TestRestTemplateConfig.class)
class ServiceIntegrationTest {
    // ...
}
```

Descripción de cada anotación:

- **@SpringBootTest**: Carga el contexto completo de Spring Boot para pruebas
- **webEnvironment = RANDOM_PORT**: Inicia el servidor en un puerto aleatorio para evitar conflictos
- **@ActiveProfiles("test")**: Activa el perfil de configuración de pruebas
- **@TestMethodOrder**: Permite ordenar la ejecución de pruebas de forma controlada
- **@Import**: Importa configuraciones adicionales específicas para pruebas

### Inyección de Dependencias

Las pruebas de integración inyectan componentes reales y simulados:

```java
@Autowired
private FavouriteService favouriteService; // Servicio real con lógica completa

@Autowired
private FavouriteRepository favouriteRepository; // Repositorio real con base de datos

@MockBean
private RestTemplate restTemplate; // Mock para servicios externos
```

### Configuración de Prueba

Cada prueba incluye un método de configuración que prepara el estado inicial:

```java
@BeforeEach
void setup() {
    reset(restTemplate); // Limpia el estado del mock
    favouriteRepository.deleteAll(); // Limpia la base de datos
}
```

### Convención de Nomenclatura

Los métodos de prueba siguen el mismo patrón que las unitarias:

```
nombreMetodo_CuandoCondicion_ComportamientoEsperado
```

Ejemplos específicos de integración:
- `save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully`
- `findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully`
- `save_WhenOrderServiceReturns404_ThrowsResourceNotFoundException`

## Escenarios de Prueba

### Persistencia y Consulta

Las pruebas verifican que los datos se persisten y consultan correctamente de la base de datos:

```java
@Test
@Order(1)
@DisplayName("save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully")
void save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully() {
    // Arrange
    UserDto mockUser = UserDto.builder()
        .userId(1)
        .firstName("John")
        .lastName("Doe")
        .email("john.doe@example.com")
        .build();
    
    when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
        .thenReturn(mockUser);
    
    CartDto toSave = CartDto.builder().userId(1).build();
    
    // Act
    CartDto saved = cartService.save(toSave);
    
    // Assert
    assertThat(saved).isNotNull();
    assertThat(saved.getCartId()).isNotNull(); // ID generado por BD
    assertThat(saved.getUserId()).isEqualTo(1);
    
    // Verificar persistencia real
    Cart persisted = cartRepository.findById(saved.getCartId()).orElse(null);
    assertThat(persisted).isNotNull();
}
```

### Integración con Servicios Externos

Las pruebas simulan respuestas de servicios externos y validan el manejo correcto:

```java
@Test
@Order(1)
@DisplayName("findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully")
void findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully() {
    // Arrange
    Integer userId = 1;
    Integer productId = 100;
    LocalDateTime likeDate = LocalDateTime.of(2024, 1, 15, 10, 30);
    
    // Persistir en base de datos real
    favouriteRepository.save(createFavourite(userId, productId, likeDate));
    
    // Simular respuestas de servicios externos
    UserDto mockUser = UserDto.builder()
        .userId(userId)
        .firstName("Alice")
        .email("alice@example.com")
        .build();
    
    ProductDto mockProduct = ProductDto.builder()
        .productId(productId)
        .productTitle("Wireless Mouse")
        .priceUnit(25.99)
        .build();
    
    when(restTemplate.getForObject(contains("/users/" + userId), eq(UserDto.class)))
        .thenReturn(mockUser);
    when(restTemplate.getForObject(contains("/products/" + productId), eq(ProductDto.class)))
        .thenReturn(mockProduct);
    
    // Act
    FavouriteId id = new FavouriteId(userId, productId, likeDate);
    FavouriteDto result = favouriteService.findById(id);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getUserDto()).isNotNull();
    assertThat(result.getUserDto().getFirstName()).isEqualTo("Alice");
    assertThat(result.getProductDto()).isNotNull();
    assertThat(result.getProductDto().getProductTitle()).isEqualTo("Wireless Mouse");
}
```

### Manejo de Errores de Servicios Externos

Las pruebas validan que el sistema maneja correctamente errores HTTP:

```java
@Test
@Order(2)
@DisplayName("save_WhenUserServiceReturns404_ThrowsResourceNotFoundException")
void save_WhenUserServiceReturns404_ThrowsResourceNotFoundException() {
    // Arrange
    when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
        .thenThrow(HttpClientErrorException.NotFound.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            null
        ));
    
    CartDto toSave = CartDto.builder().userId(999).build();
    
    // Act & Assert
    assertThatThrownBy(() -> cartService.save(toSave))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("User with id");
}
```

### Errores de Servidor Externo

Las pruebas verifican el manejo de errores 5xx:

```java
@Test
@Order(3)
@DisplayName("save_WhenUserServiceReturns500_ThrowsExternalServiceException")
void save_WhenUserServiceReturns500_ThrowsExternalServiceException() {
    // Arrange
    when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
        .thenThrow(HttpServerErrorException.InternalServerError.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Server Error",
            HttpHeaders.EMPTY,
            new byte[0],
            null
        ));
    
    CartDto toSave = CartDto.builder().userId(1).build();
    
    // Act & Assert
    assertThatThrownBy(() -> cartService.save(toSave))
        .isInstanceOf(ExternalServiceException.class)
        .hasMessageContaining("Error communicating");
}
```

### Actualización de Estado en Servicios Externos

Las pruebas validan que las operaciones afectan servicios externos:

```java
@Test
@Order(1)
@DisplayName("save_WhenOrderServiceReturnsValidOrderedStatus_SavesPaymentAndUpdatesOrderStatus")
void save_WhenOrderServiceReturnsValidOrderedStatus_SavesPaymentAndUpdatesOrderStatus() {
    // Arrange
    Integer orderId = 1000;
    OrderDto mockOrder = OrderDto.builder()
        .orderId(orderId)
        .orderStatus("ORDERED")
        .orderFee(299.99)
        .build();
    
    // Mock consulta de orden
    when(restTemplate.getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class)))
        .thenReturn(mockOrder);
    
    // Mock actualización de estado
    when(restTemplate.patchForObject(
        eq(ORDER_API + "/" + orderId + "/status"),
        isNull(),
        eq(Void.class)
    )).thenReturn(null);
    
    PaymentDto paymentDto = PaymentDto.builder()
        .orderDto(OrderDto.builder().orderId(orderId).build())
        .paymentStatus(PaymentStatus.NOT_STARTED)
        .isPayed(false)
        .build();
    
    // Act
    PaymentDto savedPayment = paymentService.save(paymentDto);
    
    // Assert
    assertThat(savedPayment).isNotNull();
    assertThat(savedPayment.getPaymentId()).isNotNull();
    
    // Verificar llamadas a servicios externos
    verify(restTemplate, times(1))
        .getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class));
    verify(restTemplate, times(1))
        .patchForObject(eq(ORDER_API + "/" + orderId + "/status"), isNull(), eq(Void.class));
}
```

### Consultas con Múltiples Servicios Externos

Las pruebas validan enriquecimiento de datos desde múltiples fuentes:

```java
@Test
@Order(1)
@DisplayName("findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully")
void findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully() {
    // Arrange
    Integer productId = 100;
    Integer orderId = 200;
    
    orderItemRepository.save(OrderItem.builder()
        .productId(productId)
        .orderId(orderId)
        .orderedQuantity(5)
        .build());
    
    ProductDto mockProduct = ProductDto.builder()
        .productId(productId)
        .productTitle("Laptop")
        .priceUnit(999.99)
        .build();
    
    OrderDto mockOrder = OrderDto.builder()
        .orderId(orderId)
        .orderFee(4999.95)
        .build();
    
    when(restTemplate.getForObject(eq(PRODUCT_API + "/" + productId), eq(ProductDto.class)))
        .thenReturn(mockProduct);
    when(restTemplate.getForObject(eq(ORDER_API + "/" + orderId), eq(OrderDto.class)))
        .thenReturn(mockOrder);
    
    // Act
    OrderItemId id = new OrderItemId(productId, orderId);
    OrderItemDto result = orderItemService.findById(id);
    
    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getProductDto()).isNotNull();
    assertThat(result.getProductDto().getProductTitle()).isEqualTo("Laptop");
    assertThat(result.getOrderDto()).isNotNull();
    assertThat(result.getOrderDto().getOrderFee()).isEqualTo(4999.95);
}
```

## Configuración de Pruebas

### Perfil de Prueba

Las pruebas utilizan un perfil específico definido en:

```
src/test/resources/application-test.yml
```

Este perfil típicamente configura:

- Base de datos H2 en memoria
- Deshabilitación de descubrimiento de servicios
- Configuración de logging para pruebas
- Deshabilitación de características específicas de producción

Ejemplo de configuración:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  cloud:
    discovery:
      enabled: false
```

### Configuración de RestTemplate para Pruebas

Algunos microservicios incluyen una configuración específica de RestTemplate para pruebas:

```java
@TestConfiguration
public class TestRestTemplateConfig {
    
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
```

Esta configuración se importa en las pruebas mediante `@Import(TestRestTemplateConfig.class)`.

## Ordenamiento de Pruebas

Las pruebas de integración utilizan la anotación `@Order` para controlar la secuencia de ejecución:

```java
@Test
@Order(1)
@DisplayName("save_WhenUserServiceReturnsValidUser_SavesCartSuccessfully")
void testOne() { }

@Test
@Order(2)
@DisplayName("save_WhenUserServiceReturns404_ThrowsResourceNotFoundException")
void testTwo() { }

@Test
@Order(3)
@DisplayName("findAll_WhenCartsExist_ReturnsEnrichedList")
void testThree() { }
```

Este ordenamiento es útil para:

- Ejecutar primero casos básicos de éxito
- Seguir con casos de validación y error
- Mantener una narrativa lógica en la ejecución

## Limpieza de Estado

Cada prueba garantiza un estado limpio mediante:

```java
@BeforeEach
void setup() {
    reset(restTemplate); // Limpia configuraciones de mock
    favouriteRepository.deleteAll(); // Limpia datos de base de datos
}
```

Esto asegura que cada prueba comienza con un estado conocido y no es afectada por pruebas anteriores.

## Ejecución de Pruebas de Integración

### Ejecutar Todas las Pruebas de Integración

Desde el directorio raíz del proyecto:

```bash
mvn verify
```

Este comando ejecuta primero las pruebas unitarias (fase `test`) y luego las pruebas de integración (fase `verify`).

### Ejecutar Solo Pruebas de Integración

Para ejecutar solo las pruebas de integración, se puede usar el plugin Failsafe (si está configurado) o especificar el patrón:

```bash
mvn test -Dtest=*IntegrationTest
```

### Ejecutar Pruebas de Integración de un Microservicio

```bash
cd <microservicio>
mvn test -Dtest=*IntegrationTest
```

Ejemplo:

```bash
cd favourite-service
mvn test -Dtest=FavouriteServiceIntegrationTest
```

### Ejecutar una Prueba Específica

```bash
mvn test -Dtest=FavouriteServiceIntegrationTest#findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully
```

### Ejecutar con Perfil de Prueba Específico

```bash
mvn test -Dspring.profiles.active=test
```

### Ejecutar con Logs Detallados

```bash
mvn test -Dtest=*IntegrationTest -Dlogging.level.root=DEBUG
```

### Ejecutar Pruebas de Integración en Paralelo

```bash
mvn test -Dtest=*IntegrationTest -DforkCount=4
```

## Interpretación de Resultados

### Resultado Exitoso

Cuando todas las pruebas de integración pasan:

```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Resultado con Fallas

Cuando hay fallas, Maven proporciona información detallada:

```
[ERROR] Tests run: 5, Failures: 1, Errors: 0, Skipped: 0
[ERROR] 
[ERROR] Failures: 
[ERROR]   FavouriteServiceIntegrationTest.save_WhenUserServiceReturns404_ThrowsResourceNotFoundException:124
[ERROR]     Expected: exception of type ResourceNotFoundException
[ERROR]     But was: ExternalServiceException
```

### Reportes de Pruebas

Los reportes detallados se generan en:

```
<microservicio>/target/surefire-reports/
```

Contenido de los reportes:

- **TEST-*.xml**: Resultados en formato XML
- ***.txt**: Salida de consola de cada clase de prueba
- Trazas de stack completas para fallas y errores

## Depuración de Pruebas de Integración

### Habilitar Logs SQL

En `application-test.yml`:

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Inspeccionar Estado de Base de Datos

Pausar ejecución para inspeccionar:

```java
@Test
void testMethod() {
    // Realizar operaciones
    cartService.save(cartDto);
    
    // Pausar para inspeccionar
    Thread.sleep(60000); // 1 minuto
}
```

### Verificar Llamadas a RestTemplate

```java
// Capturar todas las invocaciones
verify(restTemplate, atLeast(1)).getForObject(anyString(), any());

// Ver argumentos específicos
ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
verify(restTemplate).getForObject(urlCaptor.capture(), eq(UserDto.class));
System.out.println("URL llamada: " + urlCaptor.getValue());
```

## Mejores Prácticas Implementadas

### Uso de Base de Datos en Memoria

Las pruebas utilizan H2 en memoria, que se crea y destruye con cada ejecución, garantizando aislamiento completo.

### Mocks de Servicios Externos

Los servicios externos se simulan mediante `@MockBean`, permitiendo controlar sus respuestas sin dependencias externas.

### Verificación de Persistencia

Las pruebas verifican que los datos se persisten correctamente consultando directamente el repositorio.

### Pruebas de Casos de Error

Se prueban explícitamente escenarios de error como servicios externos caídos, recursos no encontrados, y errores de validación.

### Nombres Descriptivos

Los nombres de pruebas describen claramente el escenario completo de integración.

### Documentación en Comentarios

Las clases de prueba incluyen comentarios JavaDoc explicando su propósito:

```java
/**
 * Integration tests for FavouriteService verifying communication with User and Product Services.
 * Uses MockBean to simulate external service responses via RestTemplate.
 * 
 * Tests follow Arrange-Act-Assert pattern and naming convention:
 * MethodName_WhenCondition_ExpectedBehavior
 */
```

### Limpieza Entre Pruebas

El método `@BeforeEach` garantiza que cada prueba comienza con un estado limpio.

### Verificación de Interacciones

Las pruebas verifican no solo el resultado, sino también las interacciones con servicios externos.

### Uso de Builders

Los objetos de prueba se crean con builders para mayor claridad:

```java
UserDto mockUser = UserDto.builder()
    .userId(1)
    .firstName("John")
    .lastName("Doe")
    .email("john.doe@example.com")
    .build();
```

### Aserciones Múltiples pero Relacionadas

Las pruebas incluyen múltiples aserciones que validan diferentes aspectos del mismo comportamiento integrado.

## Cobertura de Escenarios

Las pruebas de integración cubren los siguientes escenarios principales:

### Operaciones CRUD Completas

- Creación de entidades con persistencia real
- Consulta de entidades desde base de datos
- Actualización de entidades existentes
- Eliminación de entidades

### Integración con Servicios Externos

- Consulta exitosa de servicios externos
- Enriquecimiento de DTOs con datos externos
- Manejo de errores HTTP 4xx
- Manejo de errores HTTP 5xx
- Actualización de estado en servicios externos

### Validaciones de Negocio

- Validación de reglas de negocio complejas
- Verificación de estados válidos
- Manejo de transiciones de estado

### Manejo de Excepciones

- Excepciones de servicios externos
- Excepciones de validación
- Excepciones de recursos no encontrados
- Excepciones de duplicados

## Limitaciones y Alcance

### Lo que Prueban

Las pruebas de integración validan:

- Integración entre servicio y repositorio
- Persistencia real en base de datos embebida
- Simulación controlada de servicios externos
- Transacciones de base de datos
- Mapeo de entidades JPA

### Lo que No Prueban

Las pruebas de integración no cubren:

- Comunicación real entre microservicios (requiere entorno completo)
- Integración con base de datos PostgreSQL de producción
- Descubrimiento de servicios con Eureka
- Configuración distribuida con Config Server
- Trazabilidad distribuida con Zipkin
- Métricas con Prometheus
- Balanceo de carga real
- Circuit breakers en escenarios reales

Estos aspectos se validan con:
- Pruebas end-to-end (e2e-tests)
- Pruebas de carga (locust-tests)
- Pruebas en entornos de staging

## Configuración de CI/CD

Las pruebas de integración se ejecutan automáticamente en el pipeline de CI/CD:

```yaml
# Ejemplo de configuración en .github/workflows
- name: Run Integration Tests
  run: mvn test -Dtest=*IntegrationTest
```

## Métricas de Calidad

Para obtener métricas de las pruebas de integración:

```bash
# Tiempo de ejecución
mvn test -Dtest=*IntegrationTest | grep "Time elapsed"

# Cantidad de pruebas
find . -name "*IntegrationTest.java" | wc -l

# Cobertura (con JaCoCo)
mvn test jacoco:report
```

## Mantenimiento

### Actualización Regular

Las pruebas de integración deben actualizarse cuando:

- Se modifica la lógica de integración
- Se agregan nuevos servicios externos
- Se modifican contratos de API
- Se actualizan dependencias

### Refactorización

Las pruebas deben refactorizarse para:

- Eliminar duplicación en configuración de mocks
- Centralizar creación de datos de prueba
- Mejorar legibilidad y mantenibilidad
- Optimizar tiempo de ejecución

### Monitoreo de Tiempo de Ejecución

Las pruebas de integración son más lentas que las unitarias. Se debe monitorear su tiempo de ejecución para:

- Identificar pruebas problemáticas
- Optimizar consultas a base de datos
- Reducir inicialización de contexto cuando sea posible

## Integración con Otras Pruebas

Las pruebas de integración complementan:

- **Pruebas Unitarias**: Validan integración, no solo lógica aislada
- **Pruebas E2E**: Validan componentes individuales antes de pruebas completas
- **Pruebas de Carga**: Proveen confianza antes de pruebas de rendimiento

## Recomendaciones

1. Ejecutar pruebas de integración antes de cada commit importante
2. Mantener el perfil de prueba actualizado con configuraciones realistas
3. Simular diferentes respuestas de servicios externos
4. Incluir casos de error y timeout
5. Verificar tanto datos persistidos como retornados
6. Mantener pruebas independientes entre sí
7. Limpiar base de datos entre pruebas
8. Documentar escenarios complejos de integración
9. Revisar y actualizar mocks cuando cambien contratos
10. Monitorear tiempo de ejecución y optimizar cuando sea necesario
11. Ejecutar suite completa en CI/CD
12. Investigar fallas inmediatamente
13. Mantener cobertura de escenarios críticos
14. Usar ordenamiento de pruebas cuando tenga sentido narrativo
15. Incluir verificaciones de interacciones con servicios externos
