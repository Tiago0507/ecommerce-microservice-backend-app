"""
Order Service performance test tasks.
Tests order management and cart operations.
"""

from locust import task, tag
import random
from datetime import datetime


class OrderTasks:
    """Order service related tasks"""
    
    @task(4)
    @tag('order', 'read')
    def get_orders(self):
        """GET /order-service/api/orders - List all orders"""
        with self.client.get(
            "/app/api/orders",
            catch_response=True,
            name="[ORDER] Get all orders"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(5)
    @tag('order', 'read')
    def get_order_by_id(self):
        """GET /order-service/api/orders/{id} - Get specific order"""
        order_id = random.choice(self.user.order_ids)
        with self.client.get(
            f"/app/api/orders/{order_id}",
            catch_response=True,
            name="[ORDER] Get order by ID"
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(2)
    @tag('order', 'write')
    def create_order(self):
        """POST /order-service/api/orders - Create new order"""
        payload = {
            "orderDate": datetime.now().strftime("%d-%m-%Y__%H:%M:%S:000000"),
            "orderDesc": f"Test Order {random.randint(1000, 9999)}",
            "orderFee": round(random.uniform(5.0, 50.0), 2),
            "cartDto": {
                "cartId": random.randint(1, 100)
            }
        }
        
        with self.client.post(
            "/app/api/orders",
            json=payload,
            catch_response=True,
            name="[ORDER] Create order"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(3)
    @tag('order', 'cart', 'read')
    def get_carts(self):
        """GET /order-service/api/carts - List shopping carts"""
        with self.client.get(
            "/app/api/carts",
            catch_response=True,
            name="[ORDER] Get all carts"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(1)
    @tag('order', 'cart', 'write')
    def create_cart(self):
        """POST /order-service/api/carts - Create shopping cart"""
        payload = {
            "userId": random.choice(self.user.user_ids)
        }
        
        with self.client.post(
            "/app/api/carts",
            json=payload,
            catch_response=True,
            name="[ORDER] Create cart"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")