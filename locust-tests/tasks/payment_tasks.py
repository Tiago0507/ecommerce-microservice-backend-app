"""
Payment Service performance test tasks.
Tests payment processing and transaction operations.
"""

from locust import task, tag
import random


class PaymentTasks:
    """Payment service related tasks"""
    
    @task(4)
    @tag('payment', 'read')
    def get_payments(self):
        """GET /payment-service/api/payments - List all payments"""
        with self.client.get(
            "/app/api/payments",
            catch_response=True,
            name="[PAYMENT] Get all payments"
        ) as response:
            if response.status_code == 200:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(5)
    @tag('payment', 'read')
    def get_payment_by_id(self):
        """GET /payment-service/api/payments/{id} - Get specific payment"""
        payment_id = random.randint(1, 200)
        with self.client.get(
            f"/app/api/payments/{payment_id}",
            catch_response=True,
            name="[PAYMENT] Get payment by ID"
        ) as response:
            if response.status_code == 200:
                response.success()
            elif response.status_code == 404:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(2)
    @tag('payment', 'write', 'critical')
    def create_payment(self):
        """POST /payment-service/api/payments - Process payment"""
        payment_modes = ["CASH", "CREDIT_CARD", "PAYPAL", "DEBIT_CARD"]
        payload = {
            "isPayed": random.choice([True, False]),
            "paymentMode": random.choice(payment_modes),
            "orderId": random.choice(self.user.order_ids)
        }
        
        with self.client.post(
            "/app/api/payments",
            json=payload,
            catch_response=True,
            name="[PAYMENT] Process payment"
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")
    
    @task(1)
    @tag('payment', 'write')
    def update_payment(self):
        """PUT /payment-service/api/payments - Update payment status"""
        payment_id = random.randint(1, 200)
        payload = {
            "paymentId": payment_id,
            "isPayed": True,
            "paymentMode": "CREDIT_CARD",
            "orderId": random.choice(self.user.order_ids)
        }
        
        with self.client.put(
            "/app/api/payments",
            json=payload,
            catch_response=True,
            name="[PAYMENT] Update payment"
        ) as response:
            if response.status_code in [200, 404]:
                response.success()
            else:
                response.failure(f"Failed with status {response.status_code}")