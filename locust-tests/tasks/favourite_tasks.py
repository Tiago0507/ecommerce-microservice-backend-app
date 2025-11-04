"""
Favourite Service performance test tasks.
Tests user favourite product management and wishlist operations.
"""

from locust import task, tag
import random
from datetime import datetime


class FavouriteTasks:
    """Favourite service related tasks"""
    
    @task(3)
    @tag('favourite', 'read')
    def get_favourites(self):
        """GET /favourite-service/api/favourites - List all favourites"""
        with self.client.get(
            "/app/api/favourites",
            catch_response=True,
            name="[FAVOURITE] Get all favourites"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(2)
    @tag('favourite', 'write')
    def create_favourite(self):
        """POST /favourite-service/api/favourites - Add to favourites"""
        payload = {
            "userId": random.choice(self.user.user_ids),
            "productId": random.choice(self.user.product_ids),
            "likeDate": datetime.now().strftime("%d-%m-%Y__%H:%M:%S:000000")
        }
        
        with self.client.post(
            "/app/api/favourites",
            json=payload,
            catch_response=True,
            name="[FAVOURITE] Add to favourites"
        ) as response:
            if response.status_code in [200, 201, 409]:  # 409 = already exists
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(4)
    @tag('favourite', 'read')
    def get_favourite_by_composite_id(self):
        """GET /favourite-service/api/favourites/{userId}/{productId}/{likeDate}"""
        user_id = random.choice(self.user.user_ids)
        product_id = random.choice(self.user.product_ids)
        like_date = "01-01-2024__10:00:00:000000"
        
        with self.client.get(
            f"/app/api/favourites/{user_id}/{product_id}/{like_date}",
            catch_response=True,
            name="[FAVOURITE] Get favourite by ID"
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(1)
    @tag('favourite', 'write')
    def delete_favourite(self):
        """DELETE /favourite-service/api/favourites - Remove from favourites"""
        user_id = random.choice(self.user.user_ids)
        product_id = random.choice(self.user.product_ids)
        like_date = "01-01-2024__10:00:00:000000"
        
        with self.client.delete(
            f"/app/api/favourites/{user_id}/{product_id}/{like_date}",
            catch_response=True,
            name="[FAVOURITE] Delete favourite"
        ) as response:
            if response.status_code in [200, 204, 404]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")