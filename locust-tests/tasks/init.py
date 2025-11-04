"""
Tasks module for Locust performance tests.
Contains modularized task definitions for each microservice.
"""

from .user_tasks import UserTasks
from .product_tasks import ProductTasks
from .order_tasks import OrderTasks
from .payment_tasks import PaymentTasks
from .favourite_tasks import FavouriteTasks

__all__ = [
    'UserTasks',
    'ProductTasks',
    'OrderTasks',
    'PaymentTasks',
    'FavouriteTasks'
]