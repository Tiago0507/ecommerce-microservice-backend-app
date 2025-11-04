"""
User Service performance test tasks.
Tests user management operations including CRUD operations.
"""

from locust import task, tag
import random


class UserTasks:
    """User service related tasks"""
    
    @task(5)
    @tag('user', 'read')
    def get_users(self):
        """GET /user-service/api/users - List all users"""
        with self.client.get(
            "/app/api/users",
            catch_response=True,
            name="[USER] Get all users"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(6)
    @tag('user', 'read')
    def get_user_by_id(self):
        """GET /user-service/api/users/{id} - Get specific user"""
        user_id = random.choice(self.user.user_ids)
        with self.client.get(
            f"/app/api/users/{user_id}",
            catch_response=True,
            name="[USER] Get user by ID"
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()  # 404 is expected for non-existent IDs
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(1)
    @tag('user', 'write')
    def create_user(self):
        """POST /user-service/api/users - Create new user"""
        random_id = random.randint(10000, 99999)
        payload = {
            "firstName": f"User{random_id}",
            "lastName": f"Test{random_id}",
            "email": f"user{random_id}@test.com",
            "phone": f"+57300{random.randint(1000000, 9999999)}"
        }
        
        with self.client.post(
            "/app/api/users",
            json=payload,
            catch_response=True,
            name="[USER] Create user"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(2)
    @tag('user', 'write')
    def update_user(self):
        """PUT /user-service/api/users - Update existing user"""
        user_id = random.choice(self.user.user_ids)
        payload = {
            "userId": user_id,
            "firstName": f"Updated{random.randint(1000, 9999)}",
            "lastName": f"User{random.randint(1000, 9999)}",
            "email": f"updated{user_id}@test.com",
            "phone": f"+57310{random.randint(1000000, 9999999)}"
        }
        
        with self.client.put(
            "/app/api/users",
            json=payload,
            catch_response=True,
            name="[USER] Update user"
        ) as response:
            if response.status_code in [200, 404]:  # 404 is acceptable
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")