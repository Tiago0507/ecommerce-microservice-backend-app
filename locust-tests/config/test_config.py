"""
Test configuration and constants for Locust performance tests.
Centralizes test data and configuration parameters.
"""

import random


class TestConfig:
    """Central configuration for all performance tests"""
    
    # API Gateway Configuration - Using PROXY-CLIENT directly as API Gateway has Eureka hostname issue
    API_GATEWAY_HOST = "http://localhost:8900"
    API_BASE_PATH = "/app/api"
    
    # Test Data Ranges - Using only VALID IDs that exist in database
    PRODUCT_IDS = [1, 2, 3, 4]   # Solo productos que existen en BD
    USER_IDS = list(range(1, 51))      # User IDs: 1-50
    ORDER_IDS = list(range(1, 201))    # Order IDs: 1-200
    PAYMENT_IDS = list(range(1, 150))  # Payment IDs: 1-150
    
    # User Behavior Simulation
    MIN_WAIT_TIME = 1  # seconds
    MAX_WAIT_TIME = 3  # seconds
    
    # Load Test Parameters
    LOAD_TEST_USERS = 100
    LOAD_TEST_SPAWN_RATE = 10
    LOAD_TEST_DURATION = "10m"
    
    # Stress Test Parameters
    STRESS_TEST_USERS = 500
    STRESS_TEST_SPAWN_RATE = 50
    STRESS_TEST_DURATION = "5m"
    
    # Spike Test Parameters
    SPIKE_TEST_USERS = 1000
    SPIKE_TEST_SPAWN_RATE = 100
    SPIKE_TEST_DURATION = "3m"
    
    @staticmethod
    def get_random_product_id():
        """Get random product ID from test range"""
        return random.choice(TestConfig.PRODUCT_IDS)
    
    @staticmethod
    def get_random_user_id():
        """Get random user ID from test range"""
        return random.choice(TestConfig.USER_IDS)
    
    @staticmethod
    def get_random_order_id():
        """Get random order ID from test range"""
        return random.choice(TestConfig.ORDER_IDS)
    
    @staticmethod
    def get_random_payment_id():
        """Get random payment ID from test range"""
        return random.choice(TestConfig.PAYMENT_IDS)


# Payment modes available in the system
PAYMENT_MODES = ["CASH", "CREDIT_CARD", "PAYPAL", "DEBIT_CARD", "BANK_TRANSFER"]

# Order statuses
ORDER_STATUSES = ["PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"]

# Product categories (if needed)
PRODUCT_CATEGORIES = ["Electronics", "Clothing", "Books", "Home", "Sports", "Toys"]