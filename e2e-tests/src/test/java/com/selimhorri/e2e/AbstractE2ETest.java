package com.selimhorri.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

@Testcontainers
public abstract class AbstractE2ETest {

    protected static Network network = Network.newNetwork();
    protected static MySQLContainer<?> mysqlContainer;
    protected static GenericContainer<?> eurekaServerContainer;
    
    protected static GenericContainer<?> userServiceContainer;
    protected static GenericContainer<?> productServiceContainer;
    protected static GenericContainer<?> orderServiceContainer;
    protected static GenericContainer<?> paymentServiceContainer;
    protected static GenericContainer<?> shippingServiceContainer;
    protected static GenericContainer<?> favouriteServiceContainer;

    protected TestRestTemplate restTemplate = new TestRestTemplate();

    @BeforeAll
    static void setupInfrastructure() {
        // Create shared network
        network = Network.newNetwork();

        // Start MySQL first
        mysqlContainer = new MySQLContainer<>("mysql:8.0")
                .withNetwork(network)
                .withNetworkAliases("mysql-db")
                .withDatabaseName("ecommerce_db")
                .withUsername("root")
                .withPassword("root")
                .withCommand("--default-authentication-plugin=mysql_native_password");
        mysqlContainer.start();

        // Start Eureka Service Discovery with longer timeout
        eurekaServerContainer = new GenericContainer<>("tiago0507/service-discovery-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("eureka-server")
                .withExposedPorts(8761)
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .waitingFor(Wait.forHttp("/eureka/apps")
                        .forPort(8761)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        eurekaServerContainer.start();

        String eurekaUrl = "http://eureka-server:8761/eureka";

        userServiceContainer = new GenericContainer<>("tiago0507/user-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("user-service")
                .withExposedPorts(8700)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        userServiceContainer.start();

        // Product Service (puerto 8500)
        productServiceContainer = new GenericContainer<>("tiago0507/product-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("product-service")
                .withExposedPorts(8500)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        productServiceContainer.start();

        // Order Service (puerto 8300)
        orderServiceContainer = new GenericContainer<>("tiago0507/order-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("order-service")
                .withExposedPorts(8300)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        orderServiceContainer.start();

        // Payment Service (puerto 8400)
        paymentServiceContainer = new GenericContainer<>("tiago0507/payment-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("payment-service")
                .withExposedPorts(8400)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        paymentServiceContainer.start();

        // Shipping Service (puerto 8600)
        shippingServiceContainer = new GenericContainer<>("tiago0507/shipping-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("shipping-service")
                .withExposedPorts(8600)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        shippingServiceContainer.start();

        // Favourite Service (puerto 8800)
        favouriteServiceContainer = new GenericContainer<>("tiago0507/favourite-service-ecommerce-boot:local")
                .withNetwork(network)
                .withNetworkAliases("favourite-service")
                .withExposedPorts(8800)
                .withEnv("SPRING_DATASOURCE_URL", "jdbc:mysql://mysql-db:3306/ecommerce_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true")
                .withEnv("SPRING_DATASOURCE_USERNAME", "root")
                .withEnv("SPRING_DATASOURCE_PASSWORD", "root")
                .withEnv("SPRING_PROFILES_ACTIVE", "dev")
                .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
                .withEnv("SPRING_CLOUD_CONFIG_IMPORT_CHECK_ENABLED", "false")
                .withEnv("EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", eurekaUrl)
                .withEnv("EUREKA_CLIENT_REGISTER_WITH_EUREKA", "false")
                .withEnv("EUREKA_CLIENT_FETCH_REGISTRY", "false")
                .waitingFor(Wait.forLogMessage(".*Started.*", 1)
                        .withStartupTimeout(Duration.ofMinutes(3)));
        favouriteServiceContainer.start();
    }

    protected String getUserServiceUrl() {
        return String.format("http://localhost:%d/user-service/api/users", 
                userServiceContainer.getMappedPort(8700));
    }

    protected String getProductServiceUrl() {
        return String.format("http://localhost:%d/product-service/api/products", 
                productServiceContainer.getMappedPort(8500));
    }

    protected String getOrderServiceUrl() {
        return String.format("http://localhost:%d/order-service/api/orders", 
                orderServiceContainer.getMappedPort(8300));
    }

    protected String getCartServiceUrl() {
        return String.format("http://localhost:%d/order-service/api/carts", 
                orderServiceContainer.getMappedPort(8300));
    }

    protected String getPaymentServiceUrl() {
        return String.format("http://localhost:%d/payment-service/api/payments", 
                paymentServiceContainer.getMappedPort(8400));
    }

    protected String getShippingServiceUrl() {
        return String.format("http://localhost:%d/shipping-service/api/shippings", 
                shippingServiceContainer.getMappedPort(8600));
    }

    protected String getFavouriteServiceUrl() {
        return String.format("http://localhost:%d/favourite-service/api/favourites", 
                favouriteServiceContainer.getMappedPort(8800));
    }
}