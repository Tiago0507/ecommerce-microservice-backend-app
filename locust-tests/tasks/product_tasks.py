"""
Product Service performance test tasks.
Tests product catalog operations including search and management.
"""

from locust import task, tag
import random


class ProductTasks:
    """Product service related tasks"""
    
    @task(10)
    @tag('product', 'read')
    def get_products(self):
        """GET /product-service/api/products - List all products"""
        with self.client.get(
            "/app/api/products",
            catch_response=True,
            name="[PRODUCT] Get all products"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(8)
    @tag('product', 'read')
    def get_product_by_id(self):
        """GET /product-service/api/products/{id} - Get specific product"""
        product_id = random.choice(self.user.product_ids)
        with self.client.get(
            f"/app/api/products/{product_id}",
            catch_response=True,
            name="[PRODUCT] Get product by ID"
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(2)
    @tag('product', 'write')
    def create_product(self):
        """POST /product-service/api/products - Create new product"""
        payload = {
            "productTitle": f"Test Product {random.randint(1000, 9999)}",
            "imageUrl": "https://example.com/image.jpg",
            "sku": f"SKU-{random.randint(10000, 99999)}",
            "priceUnit": round(random.uniform(10.0, 500.0), 2),
            "quantity": random.randint(1, 100)
        }
        
        with self.client.post(
            "/app/api/products",
            json=payload,
            catch_response=True,
            name="[PRODUCT] Create product"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(3)
    @tag('product', 'write')
    def update_product(self):
        """PUT /product-service/api/products - Update existing product"""
        product_id = random.choice(self.user.product_ids)
        payload = {
            "productId": product_id,
            "productTitle": f"Updated Product {random.randint(1000, 9999)}",
            "imageUrl": "https://example.com/updated-image.jpg",
            "sku": f"SKU-UPD-{random.randint(10000, 99999)}",
            "priceUnit": round(random.uniform(15.0, 600.0), 2),
            "quantity": random.randint(5, 150)
        }
        
        with self.client.put(
            "/app/api/products",
            json=payload,
            catch_response=True,
            name="[PRODUCT] Update product"
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")