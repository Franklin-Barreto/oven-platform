# Manual Payment Registration

## API

```
POST /api/orders/{orderId}/payments
```

### Request

```json
{
  "paymentMethod": "cash|card|pix",
  "amount": 45.90,
  "receivedAmount": 50.00,
  "cashierId": "uuid"
}
```

### Response

```json
{
  "id": "uuid",
  "amount": 45.90,
  "change": 4.10,
  "status": "confirmed",
  "registeredAt": "2026-05-16T02:30:00Z"
}
```

## Implementation

```python
class PaymentService:
    def register_manual_payment(self, order_id, method, amount, cashier_id, received=None):
        order = self.get_order(order_id)
        remaining = order.total - order.total_paid
        
        if amount > remaining + 0.01:
            raise OverpaymentError()
        
        change = None
        if method == "cash" and received:
            change = round(received - amount, 2)
        
        payment = self.create_payment(order_id, method, amount, change, cashier_id)
        
        new_paid = order.total_paid + amount
        status = "paid" if new_paid >= order.total - 0.01 else "partial"
        self.update_order_status(order_id, new_paid, status)
        
        return payment
```

## Business Rules

1. Cannot exceed remaining balance
2. Cash change auto-calculated
3. Supports partial payments
4. Order auto-updates to "paid" when fully covered
5. All payments logged with cashier ID
