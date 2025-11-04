"""
Main Locust file for e-commerce microservices performance testing.
Orchestrates all task modules and defines user behavior patterns.

Usage:
    # Interactive UI mode
    locust -f locustfile.py --host=http://localhost:8080
    
    # Headless load test
    locust -f locustfile.py --headless --users 100 --spawn-rate 10 --run-time 10m
    
    # With specific tags
    locust -f locustfile.py --tags product,user --users 50 --spawn-rate 5
    
    # Stress test
    locust -f locustfile.py --headless --users 500 --spawn-rate 50 --run-time 5m
"""

from locust import HttpUser, TaskSet, between, tag, task
from locust.contrib.fasthttp import FastHttpUser
import random
from datetime import datetime

# Import task modules
from tasks.user_tasks import UserTasks
from tasks.product_tasks import ProductTasks
from tasks.order_tasks import OrderTasks
from tasks.payment_tasks import PaymentTasks
from tasks.favourite_tasks import FavouriteTasks
from config.test_config import TestConfig


class EcommerceTasks(
    UserTasks,
    ProductTasks,
    OrderTasks,
    PaymentTasks,
    FavouriteTasks,
    TaskSet
):
    """
    Combined task set that inherits from all service task modules.
    Represents realistic user behavior across all microservices.
    """
    
    @task(3)
    @tag('integration', 'flow', 'critical')
    def user_browsing_flow(self):
        """
        Realistic user flow: Browse products and add to favourites.
        Validates inter-service communication.
        """
        # 1. Browse product catalog
        self.client.get(
            "/app/api/products",
            name="[FLOW] Browse products"
        )
        
        # 2. View details of 2-3 random products
        for _ in range(random.randint(2, 3)):
            product_id = random.choice(self.user.product_ids)
            self.client.get(
                f"/app/api/products/{product_id}",
                name="[FLOW] View product details",
                catch_response=True
            )
        
        # 3. Add one product to favourites
        payload = {
            "userId": self.user.user_id,
            "productId": random.choice(self.user.product_ids),
            "likeDate": datetime.now().strftime("%d-%m-%Y__%H:%M:%S:000000")
        }
        self.client.post(
            "/app/api/favourites",
            json=payload,
            name="[FLOW] Add to favourites",
            catch_response=True
        )
    
    @task(1)
    @tag('integration', 'flow', 'critical')
    def complete_purchase_flow(self):
        """
        Complete purchase flow: Order → Payment → Shipping.
        Critical business flow testing inter-service dependencies.
        """
        # 1. Create order
        order_payload = {
            "orderDate": datetime.now().strftime("%d-%m-%Y__%H:%M:%S:000000"),
            "orderDesc": f"Purchase flow {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(10.0, 50.0), 2),
            "cartDto": {
                "cartId": random.randint(1, 100)
            }
        }
        
        order_response = self.client.post(
            "/app/api/orders",
            json=order_payload,
            name="[FLOW] Create order",
            catch_response=True
        )
        
        if order_response.status_code in [200, 201]:
            order_id = random.choice(self.user.order_ids)
            
            # 2. Process payment
            payment_payload = {
                "isPayed": True,
                "paymentMode": "CREDIT_CARD",
                "orderId": order_id
            }
            
            payment_response = self.client.post(
                "/app/api/payments",
                json=payment_payload,
                name="[FLOW] Process payment",
                catch_response=True
            )
            
            if payment_response.status_code in [200, 201]:
                # 3. Create shipping record
                shipping_payload = {
                    "orderId": order_id,
                    "productId": random.choice(self.user.product_ids)
                }
                
                self.client.post(
                    "/app/api/shippings",
                    json=shipping_payload,
                    name="[FLOW] Create shipping",
                    catch_response=True
                )
    
    @task(2)
    @tag('integration', 'flow')
    def cart_to_checkout_flow(self):
        """
        Shopping cart flow: Create cart → Add items → Checkout.
        Tests cart and order service integration.
        """
        # 1. Create cart
        cart_payload = {
            "userId": self.user.user_id
        }
        
        cart_response = self.client.post(
            "/app/api/carts",
            json=cart_payload,
            name="[FLOW] Create cart",
            catch_response=True
        )
        
        if cart_response.status_code in [200, 201]:
            # 2. View products before adding to cart
            self.client.get(
                "/app/api/products",
                name="[FLOW] View products for cart"
            )
            
            # 3. Proceed to checkout (create order)
            order_payload = {
                "orderDate": datetime.now().strftime("%d-%m-%Y__%H:%M:%S:000000"),
                "orderDesc": "Cart checkout order",
                "orderFee": 12.99,
                "cartDto": {
                    "cartId": random.randint(1, 100)
                }
            }
            
            self.client.post(
                "/app/api/orders",
                json=order_payload,
                name="[FLOW] Checkout cart",
                catch_response=True
            )


class EcommerceUser(FastHttpUser):
    """
    Main user class using FastHttpUser for better performance.
    Represents a typical e-commerce platform user.
    """
    tasks = [EcommerceTasks]
    wait_time = between(
        TestConfig.MIN_WAIT_TIME,
        TestConfig.MAX_WAIT_TIME
    )
    host = TestConfig.API_GATEWAY_HOST
    
    # Class attributes for test data
    product_ids = TestConfig.PRODUCT_IDS
    user_ids = TestConfig.USER_IDS
    order_ids = TestConfig.ORDER_IDS
    
    def on_start(self):
        """
        Executed once per simulated user on start.
        Initializes user-specific test data.
        """
        self.user_id = random.choice(self.user_ids)
        self.product_id = random.choice(self.product_ids)
    
    def on_stop(self):
        """
        Executed once per simulated user on stop.
        Cleanup operations if needed.
        """
        pass