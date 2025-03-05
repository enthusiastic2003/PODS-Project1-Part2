import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def main():
    # Edge case tests
    test_duplicate_user_creation()
    test_zero_quantity_order()
    test_maximum_stock_order()
    test_multiple_item_order_partial_stock()
    test_delete_user_with_active_orders()
    test_order_status_transitions()
    test_wallet_edge_cases()

def create_user(userId, name, email):
    new_user = {"id": userId, "name": name, "email": email}
    return requests.post(userServiceURL + "/users", json=new_user)

def get_user(user_id):
    return requests.get(userServiceURL + f"/users/{user_id}")

def get_wallet(user_id):
    return requests.get(walletServiceURL + f"/wallets/{user_id}")

def update_wallet(user_id, action, amount):
    return requests.put(walletServiceURL + f"/wallets/{user_id}", 
                       json={"action": action, "amount": amount})

def get_product_details(product_id):
    return requests.get(marketplaceServiceURL + f"/products/{product_id}")

def create_order(user_id, items):
    new_order = {
        "user_id": user_id,
        "items": items
    }
    return requests.post(marketplaceServiceURL + "/orders", json=new_order)

def delete_users():
    requests.delete(userServiceURL + "/users")

def test_duplicate_user_creation():
    """Test creating users with duplicate emails"""
    try:
        delete_users()
        
        # Create first user
        first_response = create_user(301, "Test User", "test@example.com")
        
        # Try to create second user with same email
        second_response = create_user(302, "Test User 2", "test@example.com")
        
        if first_response.status_code == 201 and second_response.status_code == 400:
            print("Test (Duplicate User Creation): Passed")
        else:
            print("Test (Duplicate User Creation): Failed")
            
    except Exception as e:
        print(f"Test (Duplicate User Creation): Failed with exception: {str(e)}")

def test_zero_quantity_order():
    """Test placing orders with zero quantity and negative quantity"""
    try:
        delete_users()
        
        # Create user and setup
        user_response = create_user(303, "Zero Quantity", "zero@example.com")
        user_id = user_response.json()['id']
        update_wallet(user_id, "credit", 10000)
        
        # Try order with zero quantity
        zero_order = create_order(user_id, [{"product_id": 101, "quantity": 0}])
        
        # Try order with negative quantity
        negative_order = create_order(user_id, [{"product_id": 101, "quantity": -1}])
        
        if zero_order.status_code == 400 and negative_order.status_code == 400:
            print("Test (Zero/Negative Quantity Order): Passed")
        else:
            print("Test (Zero/Negative Quantity Order): Failed")
            
    except Exception as e:
        print(f"Test (Zero/Negative Quantity Order): Failed with exception: {str(e)}")

def test_maximum_stock_order():
    """Test ordering exactly available stock and one more than available"""
    try:
        delete_users()
        
        # Create user and setup
        user_response = create_user(304, "Max Stock", "max@example.com")
        user_id = user_response.json()['id']
        update_wallet(user_id, "credit", 1000000)
        
        # Get product details to know current stock
        product = get_product_details(101).json()
        current_stock = product['stock_quantity']
        
        # Order exactly available stock
        exact_order = create_order(user_id, [{"product_id": 101, "quantity": current_stock}])
        
        # Try to order one more
        excess_order = create_order(user_id, [{"product_id": 101, "quantity": 1}])
        
        if exact_order.status_code == 201 and excess_order.status_code == 400:
            print("Test (Maximum Stock Order): Passed")
        else:
            print("Test (Maximum Stock Order): Failed")
            print(exact_order.text)
            print(excess_order.text)
            
    except Exception as e:
        print(f"Test (Maximum Stock Order): Failed with exception: {str(e)}")

def test_multiple_item_order_partial_stock():
    """Test order with multiple items where some are in stock and others aren't"""
    try:
        delete_users()
        
        # Create user and setup
        user_response = create_user(305, "Partial Stock", "partial@example.com")
        user_id = user_response.json()['id']
        update_wallet(user_id, "credit", 1000000)
        
        # Get current stock of two products
        product1 = get_product_details(101).json()
        product2 = get_product_details(102).json()
        
        # Create order with one valid quantity and one excessive quantity
        items = [
            {"product_id": 101, "quantity": 1},  # Valid quantity
            {"product_id": 102, "quantity": product2['stock_quantity'] + 1}  # Excessive quantity
        ]
        
        order_response = create_order(user_id, items)
        
        # Verify that entire order failed
        if order_response.status_code == 400:
            # Verify that no stock was deducted from first product
            product1_after = get_product_details(101).json()
            if product1['stock_quantity'] == product1_after['stock_quantity']:
                print("Test (Multiple Item Partial Stock): Passed")
            else:
                print("Test (Multiple Item Partial Stock): Failed - Stock was modified")
        else:
            print("Test (Multiple Item Partial Stock): Failed - Order was accepted")
            
    except Exception as e:
        print(f"Test (Multiple Item Partial Stock): Failed with exception: {str(e)}")

def test_delete_user_with_active_orders():
    """Test deleting user who has active orders"""
    try:
        #print("\n===== TEST: Delete User With Active Orders =====\n")
        
        delete_users()

        # Step 1: Create user
        user_response = create_user(306, "Delete Test", "delete@example.com")
        if user_response.status_code != 201:
            print("User creation failed. Aborting test.")
            return

        user_id = user_response.json().get('id')

        # Step 2: Add funds to wallet
        update_wallet(user_id, "credit", 200000)

        # Step 3: Place multiple orders
        order1 = create_order(user_id, [{"product_id": 101, "quantity": 1}])
        order2 = create_order(user_id, [{"product_id": 102, "quantity": 1}])

        if order1.status_code != 201 or order2.status_code != 201:
            print("Order placement failed. Aborting test.")
            print(order1.text)
            print(order2.text)
            return
        
        order1_id = order1.json().get('order_id')
        #print(order1.json())
        #print(order2.json())
        # Step 4: Mark one order as delivered
        delivery_response = requests.put(marketplaceServiceURL + f"/orders/{order1_id}", json={"order_id": f"{order1_id}", "status": "DELIVERED"})
        #log_response(delivery_response, "Update Order to DELIVERED")

        # Step 5: Get initial stock values
        product1_before = get_product_details(101).json()
        product2_before = get_product_details(102).json()

        # Step 6: Delete user
        delete_response = requests.delete(userServiceURL + f"/users/{user_id}")
        #log_response(delete_response, "Delete User")

        # Step 7: Get final stock values
        product1_after = get_product_details(101).json()
        product2_after = get_product_details(102).json()

        # Step 8: Check if wallet was deleted
        wallet_response = get_wallet(user_id)

        # Step 9: Verification
        if (delete_response.status_code == 200 and
            product1_before['stock_quantity'] == product1_after['stock_quantity'] and  # Delivered order shouldn't restore stock
            product2_before['stock_quantity'] + 1 == product2_after['stock_quantity'] and  # Non-delivered order should restore stock
            wallet_response.status_code == 404):  # Wallet should be deleted
            print("Test (Delete User With Active Orders): Passed\n")

            
        else:
            print("Test (Delete User With Active Orders): Failed\n")
            print("Initial Stock (Product 1):", product1_before['stock_quantity'])
            print("Final Stock (Product 1):", product1_after['stock_quantity'])
            print("Initial Stock (Product 2):", product2_before['stock_quantity'])
            print("Final Stock (Product 2):", product2_after['stock_quantity'])

            print("Status: ", wallet_response.status_code)

    except Exception as e:
        print(f"Test (Delete User With Active Orders): Failed with exception: {str(e)}\n")

def test_order_status_transitions():
    """Test invalid order status transitions"""
    try:
        delete_users()
        
        # Create user and setup
        user_response = create_user(307, "Status Test", "status@example.com")
        user_id = user_response.json()['id']
        update_wallet(user_id, "credit", 100000)
        
        # Place order
        order_response = create_order(user_id, [{"product_id": 101, "quantity": 1}])
        order_id = order_response.json()['order_id']
        
        # Try to mark as delivered
        deliver_response = requests.put(marketplaceServiceURL + f"/orders/{order_id}", 
                                      json={"order_id": order_id, "status": "DELIVERED"})
        
        # Try to mark as delivered again
        second_deliver = requests.put(marketplaceServiceURL + f"/orders/{order_id}", 
                                    json={"order_id": order_id, "status": "DELIVERED"})
        
        # Try to cancel delivered order
        cancel_response = requests.delete(marketplaceServiceURL + f"/orders/{order_id}")
        
        if (deliver_response.status_code == 200 and 
            second_deliver.status_code == 400 and 
            cancel_response.status_code == 400):
            print("Test (Order Status Transitions): Passed")
        else:
            print("Test (Order Status Transitions): Failed")
            
    except Exception as e:
        print(f"Test (Order Status Transitions): Failed with exception: {str(e)}")

def test_wallet_edge_cases():
    """Test wallet operations with edge cases"""
    try:
        delete_users()
        
        # Create user
        user_response = create_user(308, "Wallet Test", "wallet@example.com")
        user_id = user_response.json()['id']
        
        # Test cases:
        # 1. Credit zero amount
        zero_credit = update_wallet(user_id, "credit", 0)
        
        # 2. Credit negative amount
        negative_credit = update_wallet(user_id, "credit", -100)
        
        # 3. Debit from empty wallet
        empty_debit = update_wallet(user_id, "debit", 100)
        
        # 4. Credit large amount and debit
        update_wallet(user_id, "credit", 1000000)
        large_debit = update_wallet(user_id, "debit", 1000000)
        
        if (zero_credit.status_code == 200 and 
            negative_credit.status_code == 400 and
            empty_debit.status_code == 400 and
            large_debit.status_code == 200):
            print("Test (Wallet Edge Cases): Passed")
        else:
            print("Test (Wallet Edge Cases): Failed")
            
    except Exception as e:
        print(f"Test (Wallet Edge Cases): Failed with exception: {str(e)}")

def log_response(response, message="Response"):
    """Helper function to log HTTP response details"""
    try:
        print(f"\n{message}:")
        print(f"Status Code: {response.status_code}")
        print(f"Response Body: {response.json()}\n")
    except Exception:
        print(f"\n{message}:")
        print(f"Status Code: {response.status_code}")
        print(f"Response Body: {response.text}\n")


if __name__ == "__main__":
    main()