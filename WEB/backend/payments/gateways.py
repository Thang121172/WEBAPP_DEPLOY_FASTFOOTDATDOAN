# Payment gateway helper placeholders (VNPay/MoMo)

def create_payment_url(order_id, amount):
    return f"https://payments.example/checkout?order={order_id}&amount={amount}"


def verify_callback(params):
    return True
