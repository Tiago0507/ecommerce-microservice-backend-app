# Pruebas de Integración - Microservicios

## Resumen

Se han implementado **10 pruebas de integración** (5 por servicio) que validan la comunicación entre microservicios siguiendo las mejores prácticas:

- **Patrón Arrange-Act-Assert (AAA)**
- **Nomenclatura:** `MethodName_WhenCondition_ExpectedBehavior`
- **Aislamiento:** Uso de `@MockBean` para simular servicios externos
- **Limpieza:** `@BeforeEach` para resetear mocks y limpiar repositorios

---

## Shipping Service - Integration Tests

**Archivo:** `shipping-service/src/test/java/com/selimhorri/app/integration/OrderItemServiceIntegrationTest.java`

**Comunicación validada:**
- Shipping Service ↔ Product Service
- Shipping Service ↔ Order Service

### Pruebas implementadas:

1. **`findById_WhenProductAndOrderServicesReturnValidData_EnrichesOrderItemSuccessfully`**
   - **Escenario:** Product Service y Order Service devuelven datos válidos
   - **Validación:** OrderItem se enriquece correctamente con datos de producto y orden
   - **Interacción:** GET /products/{id} y GET /orders/{id}

2. **`findById_WhenProductServiceReturns404_ThrowsExternalServiceException`**
   - **Escenario:** Product Service devuelve 404 (producto no encontrado)
   - **Validación:** Se lanza `ExternalServiceException` apropiada
   - **Manejo de errores:** Verifica que el servicio maneja correctamente productos inexistentes

3. **`findById_WhenOrderServiceReturns404_ThrowsExternalServiceException`**
   - **Escenario:** Order Service devuelve 404 (orden no encontrada)
   - **Validación:** Se lanza `ExternalServiceException` apropiada
   - **Manejo de errores:** Verifica que el servicio maneja correctamente órdenes inexistentes

4. **`findAll_WhenProductAndOrderServicesReturnValidData_EnrichesAllOrderItems`**
   - **Escenario:** Múltiples OrderItems con servicios externos funcionando correctamente
   - **Validación:** Todos los items se enriquecen con datos de productos y órdenes
   - **Escalabilidad:** Verifica el comportamiento con múltiples entidades

5. **`findById_WhenProductServiceUnavailable_ThrowsExternalServiceException`**
   - **Escenario:** Product Service no disponible (503 Service Unavailable)
   - **Validación:** Se lanza `ExternalServiceException` con mensaje apropiado
   - **Resiliencia:** Verifica el manejo de servicios caídos

---

## Favourite Service - Integration Tests

**Archivo:** `favourite-service/src/test/java/com/selimhorri/app/integration/FavouriteServiceIntegrationTest.java`

**Comunicación validada:**
- Favourite Service ↔ User Service
- Favourite Service ↔ Product Service

### Pruebas implementadas:

1. **`findById_WhenUserAndProductServicesReturnValidData_EnrichesFavouriteSuccessfully`**
   - **Escenario:** User Service y Product Service devuelven datos válidos
   - **Validación:** Favourite se enriquece correctamente con datos de usuario y producto
   - **Interacción:** GET /users/{id} y GET /products/{id}

2. **`findById_WhenUserServiceReturns404_ThrowsExternalServiceException`**
   - **Escenario:** User Service devuelve 404 (usuario no encontrado)
   - **Validación:** Se lanza `ExternalServiceException` apropiada
   - **Manejo de errores:** Verifica que el servicio maneja correctamente usuarios inexistentes

3. **`findById_WhenProductServiceReturns404_ThrowsExternalServiceException`**
   - **Escenario:** Product Service devuelve 404 (producto no encontrado)
   - **Validación:** Se lanza `ExternalServiceException` apropiada
   - **Manejo de errores:** Verifica que el servicio maneja correctamente productos inexistentes

4. **`findAll_WhenUserAndProductServicesReturnValidData_EnrichesAllFavourites`**
   - **Escenario:** Múltiples Favourites con servicios externos funcionando correctamente
   - **Validación:** Todos los favoritos se enriquecen con datos de usuarios y productos
   - **Escalabilidad:** Verifica el comportamiento con múltiples entidades

5. **`findById_WhenUserServiceUnavailable_ThrowsExternalServiceException`**
   - **Escenario:** User Service no disponible (503 Service Unavailable)
   - **Validación:** Se lanza `ExternalServiceException` con mensaje apropiado
   - **Resiliencia:** Verifica el manejo de servicios caídos

---

## Tecnologías utilizadas

- **Spring Boot Test:** Framework de pruebas
- **JUnit 5:** Motor de ejecución de pruebas
- **Mockito:** Mocking de dependencias externas
- **AssertJ:** Assertions fluidas y legibles
- **H2 Database:** Base de datos en memoria para pruebas
- **RestTemplate:** Cliente HTTP mockeado para simular comunicación entre servicios

---

## Configuración de las pruebas

### Annotations principales:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
```

- **@SpringBootTest:** Levanta el contexto completo de Spring
- **@ActiveProfiles("test"):** Usa el perfil de test (H2 en memoria)
- **@TestMethodOrder:** Ejecuta las pruebas en orden específico

### Setup común:

```java
@BeforeEach
void setup() {
    reset(restTemplate);
    [repository].deleteAll();
}
```

Asegura que cada prueba comienza con un estado limpio.

---

## Ejecución de las pruebas

### Shipping Service:
```bash
cd shipping-service
mvn test -Dtest=OrderItemServiceIntegrationTest
```

### Favourite Service:
```bash
cd favourite-service
mvn test -Dtest=FavouriteServiceIntegrationTest
```

### Ambos servicios:
```bash
mvn test -pl shipping-service,favourite-service -Dtest=*IntegrationTest
```

---

## Cobertura de casos de prueba

### ✅ Casos exitosos:
- Comunicación correcta entre servicios
- Enriquecimiento de datos desde servicios externos
- Operaciones con múltiples entidades

### ✅ Casos de error:
- Recursos no encontrados (404)
- Servicios no disponibles (503)
- Validación de excepciones personalizadas

### ✅ Verificaciones:
- Llamadas al RestTemplate con parámetros correctos
- Número de invocaciones a servicios externos
- Estructura de datos devuelta
- Mensajes de error apropiados

---

## Próximos pasos recomendados

1. ✅ **Pruebas unitarias** - Completadas en todos los microservicios
2. ✅ **Pruebas de integración** - Completadas para Shipping y Favourite (este documento)
3. ⏳ **Pruebas E2E** - Flujos completos de usuario
4. ⏳ **Pruebas de rendimiento** - Locust para simular carga

---

## Cumplimiento del Taller

Estas pruebas cumplen con el requisito:
> "Al menos cinco nuevas pruebas de integración que validen la comunicación entre servicios"

**Estado:** ✅ **10/5 pruebas completadas** (200% del requisito)

- Shipping Service: 5 pruebas ✅
- Favourite Service: 5 pruebas ✅
