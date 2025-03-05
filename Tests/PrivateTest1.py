import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def main():
    # Test 1: Basic order flow with discount
    test_basic_order_flow()
    
    # Test 2: Order cancellation flow
    test_order_cancellation()
    
    # Test 3: Multiple orders - verify discount applies only once
    test_multiple_orders_discount()
    
    # Test 4: Insufficient wallet balance
    test_insufficient_balance()
    
    # Test 5: Order delivery flow
    test_order_delivery()

def create_user(userId, name, email):
    new_user = {"id": userId, "name": name, "email": email}
    return requests.post(userServiceURL + "/users", json=new_user)

def get_wallet(user_id):
    return requests.get(walletServiceURL + f"/wallets/{user_id}")

def update_wallet(user_id, action, amount):
    return requests.put(walletServiceURL + f"/wallets/{user_id}", 
                       json={"action": action, "amount": amount})

def get_product_details(product_id):
    response = requests.get(marketplaceServiceURL + f"/products/{product_id}")
    if response.status_code != 200:
        print("Wrong status code returned during get product details")
        sys.exit()
    return response

def create_order(user_id, product_id, quantity):
    new_order = {
        "user_id": user_id,
        "items": [{"product_id": product_id, "quantity": quantity}]
    }
    return requests.post(marketplaceServiceURL + "/orders", json=new_order)

def delete_users():
    requests.delete(userServiceURL + "/users")

def test_basic_order_flow():
    try:
        delete_users()
        # Create user and setup
        user_response = create_user(201, "Alice Smith", "alice@example.com")
        user_id = user_response.json()['id']
        product_id = 101
        wallet_amount = 100000
        
        # Add money to wallet
        update_wallet(user_id, "credit", wallet_amount)
        
        # Get initial states
        product_before = get_product_details(product_id)
        wallet_before = get_wallet(user_id).json()['balance']
        
        # Place order
        order_response = create_order(user_id, product_id, 1)
        
        # Get final states
        product_after = get_product_details(product_id)
        wallet_after = get_wallet(user_id).json()['balance']
        
        # Verify
        price = product_before.json()['price']
        discounted_price = price * 0.9  # 10% discount
        
        if (wallet_before - wallet_after == discounted_price and 
            product_before.json()['stock_quantity'] - product_after.json()['stock_quantity'] == 1):
            print("Test 1 (Basic Order Flow): Passed")
        else:
            print("Test 1 (Basic Order Flow): Failed")
            
    except Exception as e:
        print(f"Test 1 (Basic Order Flow): Failed with exception: {str(e)}")

def test_order_cancellation():
    try:
        delete_users()
        # Create user and setup
        user_response = create_user(202, "Bob Jones", "bob@example.com")
        user_id = user_response.json()['id']
        product_id = 102
        wallet_amount = 100000
        
        # Add money to wallet
        update_wallet(user_id, "credit", wallet_amount)
        
        # Get initial states
        product_before = get_product_details(product_id)
        wallet_before = get_wallet(user_id).json()['balance']
        
        # Place order
        order_response = create_order(user_id, product_id, 1)
        order_id = order_response.json()['order_id']
        
        # Cancel order
        cancel_response = requests.delete(marketplaceServiceURL + f"/orders/{order_id}")
        
        # Get final states
        product_after = get_product_details(product_id)
        wallet_after = get_wallet(user_id).json()['balance']
        
        # Verify
        if (wallet_before == wallet_after and 
            product_before.json()['stock_quantity'] == product_after.json()['stock_quantity']):
            print("Test 2 (Order Cancellation): Passed")
        else:
            print("Test 2 (Order Cancellation): Failed")
            
    except Exception as e:
        print(f"Test 2 (Order Cancellation): Failed with exception: {str(e)}")

def test_multiple_orders_discount():
    try:
        delete_users()
        # Create user and setup
        user_response = create_user(203, "Carol White", "carol@example.com")
        user_id = user_response.json()['id']
        product_id = 103
        wallet_amount = 100000
        
        # Add money to wallet
        update_wallet(user_id, "credit", wallet_amount)
        
        # Place first order (should get discount)
        wallet_before_first = get_wallet(user_id).json()['balance']
        first_order = create_order(user_id, product_id, 1)
        wallet_after_first = get_wallet(user_id).json()['balance']
        
        # Place second order (should NOT get discount)
        wallet_before_second = wallet_after_first
        second_order = create_order(user_id, product_id, 1)
        wallet_after_second = get_wallet(user_id).json()['balance']
        
        # Get product price
        product_details = get_product_details(product_id)
        price = product_details.json()['price']
        
        # Verify
        first_order_cost = wallet_before_first - wallet_after_first
        second_order_cost = wallet_before_second - wallet_after_second
        
        if (first_order_cost == price * 0.9 and second_order_cost == price):
            print("Test 3 (Multiple Orders Discount): Passed")
        else:
            print("Test 3 (Multiple Orders Discount): Failed")
            
    except Exception as e:
        print(f"Test 3 (Multiple Orders Discount): Failed with exception: {str(e)}")

def test_insufficient_balance():
    try:
        delete_users()
        # Create user and setup
        user_response = create_user(204, "Dave Brown", "dave@example.com")
        user_id = user_response.json()['id']
        product_id = 104
        
        # Add insufficient money to wallet
        update_wallet(user_id, "credit", 100)  # Very small amount
        
        # Try to place order
        order_response = create_order(user_id, product_id, 1)
        
        # Verify
        if order_response.status_code == 400:
            print("Test 4 (Insufficient Balance): Passed")
        else:
            print("Test 4 (Insufficient Balance): Failed")
            
    except Exception as e:
        print(f"Test 4 (Insufficient Balance): Failed with exception: {str(e)}")

def test_order_delivery():
    try:
        delete_users()
        # Create user and setup
        user_response = create_user(205, "Eve Green", "eve@example.com")
        user_id = user_response.json()['id']
        product_id = 105
        wallet_amount = 100000
        
        # Add money to wallet
        update_wallet(user_id, "credit", wallet_amount)
        
        # Place order
        order_response = create_order(user_id, product_id, 1)
        order_id = order_response.json()['order_id']
        
        # Mark order as delivered
        delivery_response = requests.put(marketplaceServiceURL + f"/orders/{order_id}", 
                                       json={"order_id": order_id, "status": "DELIVERED"})
        
        # Try to cancel delivered order (should fail)
        cancel_response = requests.delete(marketplaceServiceURL + f"/orders/{order_id}")
        
        # Verify
        if (delivery_response.status_code == 200 and cancel_response.status_code == 400):
            print("Test 5 (Order Delivery): Passed")
        else:
            print("Test 5 (Order Delivery): Failed")
            
    except Exception as e:
        print(f"Test 5 (Order Delivery): Failed with exception: {str(e)}")

if __name__ == "__main__":
    main()
