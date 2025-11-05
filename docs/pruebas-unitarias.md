# Documentación de Pruebas Unitarias

## Descripción General

Las pruebas unitarias del proyecto se centran en verificar el comportamiento de componentes individuales de manera aislada, específicamente los servicios de negocio de cada microservicio. Estas pruebas utilizan objetos simulados (mocks) para aislar la unidad bajo prueba y garantizar que cada método de servicio funciona correctamente de forma independiente.

## Estructura y Ubicación

Las pruebas unitarias se encuentran organizadas en la siguiente estructura dentro de cada microservicio:

```
<microservicio>/src/test/java/com/selimhorri/app/service/impl/
```

Cada archivo de prueba sigue la convención de nomenclatura `<NombreServicio>ImplTest.java`, indicando que se prueba la implementación concreta del servicio.

### Microservicios con Pruebas Unitarias

El proyecto cuenta con pruebas unitarias en los siguientes microservicios:

- **user-service**: Contiene pruebas para `UserServiceImpl`, `AddressServiceImpl` y `CredentialServiceImpl`
- **product-service**: Incluye pruebas para `ProductServiceImpl` y `CategoryServiceImpl`
- **order-service**: Contiene pruebas para `OrderServiceImpl` y `CartServiceImpl`
- **payment-service**: Incluye pruebas para `PaymentServiceImpl`
- **shipping-service**: Contiene pruebas para `OrderItemServiceImpl`
- **favourite-service**: Incluye pruebas para `FavouriteServiceImpl`

## Tecnologías y Herramientas

Las pruebas unitarias utilizan las siguientes tecnologías:

- **JUnit 5 (Jupiter)**: Framework principal para la ejecución de pruebas
- **Mockito**: Framework de simulación para crear objetos mock
- **AssertJ**: Librería de aserciones fluidas para validaciones más expresivas
- **Spring Boot Test**: Proporciona soporte adicional para pruebas en aplicaciones Spring
- **MockitoExtension**: Extensión de JUnit 5 que habilita el uso de anotaciones de Mockito

## Patrón de Diseño

### Estructura de las Pruebas

Todas las pruebas unitarias siguen un patrón consistente que incluye:

1. **Anotación de clase**: `@ExtendWith(MockitoExtension.class)` para habilitar el uso de Mockito
2. **Objetos mock**: Dependencias simuladas anotadas con `@Mock`
3. **Servicio bajo prueba**: Instancia del servicio anotada con `@InjectMocks` que recibe las dependencias simuladas
4. **Método de configuración**: `@BeforeEach` para inicializar el estado antes de cada prueba
5. **Métodos de prueba**: Anotados con `@Test` y opcionalmente `@DisplayName`

### Convención de Nomenclatura

Los métodos de prueba siguen la convención descriptiva:

```
nombreMetodo_CuandoCondicion_ComportamientoEsperado
```

Ejemplos:
- `findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList`
- `save_WhenEmailAlreadyRegistered_ThrowsDuplicateResourceException`
- `findById_WhenProductMissing_ThrowsResourceNotFoundException`

### Patrón Arrange-Act-Assert

Cada prueba se estructura en tres secciones claramente definidas:

```java
@Test
void nombrePrueba() {
    // Arrange: Configuración de datos de prueba y comportamiento de mocks
    when(repository.findById(1)).thenReturn(Optional.of(entidad));
    
    // Act: Ejecución del método bajo prueba
    ResultadoDto resultado = servicio.metodo(parametro);
    
    // Assert: Verificación de resultados y comportamiento
    assertThat(resultado).isNotNull();
    verify(repository).findById(1);
}
```

## Casos de Prueba Comunes

### Operaciones de Consulta

Las pruebas verifican escenarios como:

- Consulta exitosa de recursos existentes
- Consulta de recursos inexistentes que lanza excepciones
- Enriquecimiento de DTOs con información de servicios externos
- Filtrado y mapeo correcto de colecciones

Ejemplo:

```java
@Test
void findAll_WhenProductsExist_ReturnsMappedDtos() {
    // Arrange
    when(productRepository.findAllWithoutDeleted())
        .thenReturn(Arrays.asList(existingProduct));
    
    // Act
    List<ProductDto> result = productService.findAll();
    
    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getProductTitle()).isEqualTo("Phone");
}
```

### Operaciones de Creación

Las pruebas validan:

- Creación exitosa de nuevos recursos
- Validación de datos de entrada obligatorios
- Detección de duplicados (email, username, etc.)
- Persistencia correcta en el repositorio
- Manejo de violaciones de integridad de datos

Ejemplo:

```java
@Test
void save_WhenEmailAlreadyRegistered_ThrowsDuplicateResourceException() {
    // Arrange
    UserDto input = UserDto.builder()
        .email("john.doe@example.com")
        .build();
    when(userRepository.existsByEmailIgnoreCase("john.doe@example.com"))
        .thenReturn(true);
    
    // Act + Assert
    DuplicateResourceException ex = assertThrows(
        DuplicateResourceException.class,
        () -> userService.save(input)
    );
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
    verify(userRepository, never()).save(any());
}
```

### Operaciones de Actualización

Las pruebas verifican:

- Actualización exitosa de recursos existentes
- Validación de existencia del recurso antes de actualizar
- Actualización parcial de campos
- Manejo de estados y transiciones válidas

### Operaciones de Eliminación

Las pruebas comprueban:

- Eliminación lógica (soft delete) de recursos
- Validación de existencia antes de eliminar
- Manejo de dependencias y relaciones

### Manejo de Servicios Externos

Las pruebas simulan y validan:

- Respuestas exitosas de servicios externos mediante RestTemplate
- Errores HTTP (404, 500, etc.) de servicios externos
- Manejo de timeouts y excepciones de red
- Enriquecimiento de DTOs con datos externos

Ejemplo:

```java
@Test
void findAll_WhenExternalServicesFetchSuccessful_EnrichesAndReturnsList() {
    // Arrange
    when(favouriteRepository.findAll()).thenReturn(Arrays.asList(
        favourite(1, 100),
        favourite(2, 200)
    ));
    
    when(restTemplate.getForObject(USER_API + "/1", UserDto.class))
        .thenReturn(user(1, "John", "john@example.com"));
    when(restTemplate.getForObject(PRODUCT_API + "/100", ProductDto.class))
        .thenReturn(product(100, "Laptop"));
    
    // Act
    List<FavouriteDto> result = favouriteService.findAll();
    
    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).allSatisfy(dto -> {
        assertThat(dto.getUserDto()).isNotNull();
        assertThat(dto.getProductDto()).isNotNull();
    });
}
```

## Validación de Excepciones

Las pruebas unitarias validan el manejo correcto de excepciones personalizadas:

- **ResourceNotFoundException**: Cuando no se encuentra un recurso solicitado
- **DuplicateResourceException**: Cuando se intenta crear un recurso duplicado
- **InvalidInputException**: Cuando los datos de entrada son inválidos
- **ExternalServiceException**: Cuando falla la comunicación con servicios externos
- **InvalidPaymentStatusException**: Cuando el estado de pago es incorrecto

Ejemplo de validación de excepciones:

```java
@Test
void save_WhenMissingMandatoryFields_ThrowsInvalidInputException() {
    // Arrange
    ProductDto dto = ProductDto.builder()
        .productTitle("") // campo obligatorio vacío
        .build();
    
    // Act + Assert
    assertThatThrownBy(() -> productService.save(dto))
        .isInstanceOf(InvalidInputException.class)
        .hasMessageContaining("mandatory fields");
}
```

## Verificación de Interacciones

Las pruebas unitarias verifican que las interacciones con dependencias ocurren correctamente mediante Mockito:

```java
// Verificar que un método fue llamado exactamente una vez
verify(repository).findById(1);

// Verificar que un método fue llamado un número específico de veces
verify(restTemplate, times(2)).getForObject(anyString(), eq(UserDto.class));

// Verificar que un método nunca fue llamado
verify(repository, never()).save(any());

// Capturar argumentos pasados a métodos
ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
verify(paymentRepository).save(captor.capture());
assertThat(captor.getValue().getPaymentStatus()).isEqualTo(PaymentStatus.NOT_STARTED);
```

## Datos de Prueba

Las pruebas utilizan métodos auxiliares para crear objetos de prueba de manera consistente:

```java
private User createUser(Integer id, String email) {
    return User.builder()
        .userId(id)
        .firstName("John")
        .lastName("Doe")
        .email(email)
        .phone("123456789")
        .build();
}

private ProductDto createProductDto(Integer id, String title) {
    return ProductDto.builder()
        .productId(id)
        .productTitle(title)
        .sku("SKU-" + id)
        .priceUnit(99.99)
        .build();
}
```

## Ejecución de Pruebas Unitarias

### Ejecutar Todas las Pruebas Unitarias del Proyecto

Desde el directorio raíz del proyecto:

```bash
mvn test
```

Este comando ejecuta todas las pruebas unitarias de todos los microservicios.

### Ejecutar Pruebas de un Microservicio Específico

```bash
cd <microservicio>
mvn test
```

Por ejemplo, para ejecutar las pruebas del servicio de usuarios:

```bash
cd user-service
mvn test
```

### Ejecutar una Clase de Prueba Específica

```bash
mvn test -Dtest=<NombreClase>Test
```

Ejemplo:

```bash
mvn test -Dtest=UserServiceImplTest
```

### Ejecutar un Método de Prueba Específico

```bash
mvn test -Dtest=<NombreClase>Test#nombreMetodo
```

Ejemplo:

```bash
mvn test -Dtest=UserServiceImplTest#save_WhenEmailAlreadyRegistered_ThrowsDuplicateResourceException
```

### Ejecutar Pruebas en Modo Silencioso

```bash
mvn test -q
```

### Ejecutar Pruebas con Reportes Detallados

```bash
mvn test -Dsurefire.printSummary=true
```

### Saltar Pruebas Durante el Build

```bash
mvn clean install -DskipTests
```

## Interpretación de Resultados

### Resultado Exitoso

Cuando todas las pruebas pasan, Maven muestra:

```
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Resultado con Fallas

Si alguna prueba falla, Maven muestra:

```
[ERROR] Tests run: 15, Failures: 1, Errors: 0, Skipped: 0
[ERROR] FAILURE: test method name
[ERROR] Expected: <expected value>
[ERROR] Actual: <actual value>
```

Los reportes detallados se generan en:

```
<microservicio>/target/surefire-reports/
```

Este directorio contiene:
- Archivos XML con resultados detallados
- Archivos TXT con salida de consola de cada prueba

## Cobertura de Código

Para generar reportes de cobertura de código, se puede utilizar JaCoCo (si está configurado):

```bash
mvn test jacoco:report
```

Los reportes se generan en:

```
<microservicio>/target/site/jacoco/index.html
```

## Mejores Prácticas Implementadas

### Aislamiento de Pruebas

Cada prueba es completamente independiente y no depende del estado de otras pruebas. El método `@BeforeEach` garantiza que cada prueba comienza con un estado limpio.

### Nombres Descriptivos

Los nombres de los métodos de prueba describen claramente qué se está probando, bajo qué condiciones y cuál es el resultado esperado.

### Una Aserción por Concepto

Aunque una prueba puede tener múltiples aserciones, todas deben verificar un mismo concepto o comportamiento.

### Uso de AssertJ

Las aserciones utilizan AssertJ para mayor legibilidad:

```java
// Preferido
assertThat(result).isNotNull();
assertThat(result.getSize()).isEqualTo(5);

// En lugar de
assertNotNull(result);
assertEquals(5, result.getSize());
```

### Verificación de Interacciones

Las pruebas no solo verifican valores de retorno, sino también que las interacciones con dependencias ocurren correctamente.

### Manejo Explícito de Excepciones

Las excepciones esperadas se validan explícitamente, verificando tanto el tipo como el mensaje:

```java
assertThatThrownBy(() -> service.method())
    .isInstanceOf(SpecificException.class)
    .hasMessageContaining("expected message");
```

### Datos de Prueba Legibles

Los datos de prueba son claros y representativos de casos reales, facilitando la comprensión del escenario.

### Limpieza de Mocks

El método `@BeforeEach` limpia el estado de los mocks para evitar interferencias entre pruebas:

```java
@BeforeEach
void resetMocks() {
    clearInvocations(repository, restTemplate);
}
```

## Mantenimiento de Pruebas

### Actualización de Pruebas

Cuando se modifica la lógica de negocio, las pruebas unitarias correspondientes deben actualizarse para reflejar los nuevos comportamientos esperados.

### Refactorización

Las pruebas deben refactorizarse junto con el código de producción para mantener la claridad y evitar duplicación.

### Eliminación de Pruebas Obsoletas

Las pruebas que ya no son relevantes deben eliminarse para mantener la suite de pruebas limpia y enfocada.

## Limitaciones de las Pruebas Unitarias

Las pruebas unitarias tienen las siguientes limitaciones que se abordan con pruebas de integración:

- No prueban la integración real con bases de datos
- No validan la comunicación real entre microservicios
- No verifican la configuración de Spring Boot
- No prueban el comportamiento del sistema completo
- No validan aspectos transaccionales reales

Estas limitaciones se complementan con las pruebas de integración que utilizan el contexto completo de Spring Boot y bases de datos embebidas.

## Estadísticas del Proyecto

El proyecto cuenta con aproximadamente:

- 58 archivos de prueba en total (unitarias e integración)
- Cobertura de todos los servicios principales de negocio
- Pruebas para casos exitosos y casos de error
- Validación de interacciones con servicios externos mediante mocks

## Recomendaciones

1. Ejecutar las pruebas unitarias frecuentemente durante el desarrollo
2. Mantener las pruebas rápidas y enfocadas
3. Agregar pruebas para cada nueva funcionalidad
4. Actualizar pruebas cuando se modifica el código existente
5. Revisar regularmente la cobertura de código
6. Utilizar las pruebas como documentación del comportamiento esperado
7. Mantener los datos de prueba simples y representativos
8. Verificar tanto casos exitosos como casos de error
9. Ejecutar la suite completa antes de realizar commits
10. Revisar los reportes de pruebas para identificar patrones de falla
