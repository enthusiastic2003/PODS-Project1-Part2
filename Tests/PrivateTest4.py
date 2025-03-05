import requests
import sys
import time

# Service URLs
USER_SERVICE = "http://localhost:8080"
MARKETPLACE_SERVICE = "http://localhost:8081"
WALLET_SERVICE = "http://localhost:8082"

def test_user_operations():
    """Test suite for user-related operations"""
    print("\n=== Testing User Operations ===")
    
    # Reset system state
    requests.delete(f"{USER_SERVICE}/users")
    
    # Test 1: Create user successfully
    print("\nTest 1: Creating new user")
    user_data = {
        "id": 1001,
        "name": "Alice Smith",
        "email": "alice.smith@example.com"
    }
    response = requests.post(f"{USER_SERVICE}/users", json=user_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code}"
    assert response.json()["discount_availed"] == False, "New user should not have availed discount"
    print("✓ User created successfully")
    
    # Test 2: Create duplicate user (same email)
    print("\nTest 2: Attempting to create duplicate user")
    duplicate_user = {
        "id": 1002,
        "name": "Alice Clone",
        "email": "alice.smith@example.com"
    }
    response = requests.post(f"{USER_SERVICE}/users", json=duplicate_user)
    assert response.status_code == 400, f"Expected 400, got {response.status_code}"
    print("✓ Duplicate user creation prevented")
    
    # Test 3: Get user details
    print("\nTest 3: Retrieving user details")
    response = requests.get(f"{USER_SERVICE}/users/1001")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    assert response.json()["name"] == "Alice Smith", "User details mismatch"
    print("✓ User details retrieved successfully")

def test_wallet_operations():
    """Test suite for wallet-related operations"""
    print("\n=== Testing Wallet Operations ===")
    
    # Test 4: Initialize wallet
    print("\nTest 4: Initializing wallet")
    response = requests.put(f"{WALLET_SERVICE}/wallets/1001", 
                          json={"action": "credit", "amount": 10000})
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    assert response.json()["balance"] == 10000, "Wallet balance incorrect"
    print("✓ Wallet initialized successfully")
    
    # Test 5: Attempt overdraft
    print("\nTest 5: Testing overdraft prevention")
    response = requests.put(f"{WALLET_SERVICE}/wallets/1001", 
                          json={"action": "debit", "amount": 20000})
    assert response.status_code == 400, f"Expected 400, got {response.status_code}"
    print("✓ Overdraft prevented successfully")

def test_order_operations():
    """Test suite for order-related operations"""
    print("\n=== Testing Order Operations ===")

    # Insert Money into Wallet
    response = requests.put(f"{WALLET_SERVICE}/wallets/1001",
                            json={"action": "credit", "amount": 1000000})
    
    # Get initial product stock
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/101")
    initial_stock = response.json()["stock_quantity"]
    initial_price = response.json()["price"]
    
    # Test 6: Place first order (with discount)
    print("\nTest 6: Placing first order (should get discount)")
    order_data = {
        "user_id": 1001,
        "items": [
            {"product_id": 101, "quantity": 1}
        ]
    }
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code} , {response.text}"
    assert response.json()["total_price"] == int(initial_price * 0.9), "Discount not applied correctly"
    first_order_id = response.json()["order_id"]
    print("✓ First order placed with discount")
    
    # Test 7: Place second order (no discount)
    print("\nTest 7: Placing second order (should not get discount)")
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code}"
    assert response.json()["total_price"] == initial_price, "Incorrect price for second order"
    second_order_id = response.json()["order_id"]
    print("✓ Second order placed without discount")
    
    # Test 8: Check stock reduction
    print("\nTest 8: Verifying stock reduction")
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/101")
    assert response.json()["stock_quantity"] == initial_stock - 2, "Stock not reduced correctly"
    print("✓ Stock reduced correctly")

def test_complex_deletion_scenarios():
    """Test suite for complex deletion scenarios"""
    print("\n=== Testing Complex Deletion Scenarios ===")
    
    # Test 11: Delete user with active orders
    print("\nTest 11: Testing user deletion with active orders")
    
    # Create new user
    user_data = {
        "id": 2001,
        "name": "Bob Johnson",
        "email": "bob.johnson@example.com"
    }
    response = requests.post(f"{USER_SERVICE}/users", json=user_data)
    user_id = response.json()["id"]
    print("✓ User created successfully")
    
    # Initialize wallet
    requests.put(f"{WALLET_SERVICE}/wallets/{user_id}", 
                json={"action": "credit", "amount": 2000000})
    
    # Place multiple orders
    order_data = {
        "user_id": user_id,
        "items": [
            {"product_id": 101, "quantity": 1},
            {"product_id": 102, "quantity": 2}
        ]
    }
    
    # Place first order
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    first_order_id = response.json()["order_id"]
    print("✓ First order placed successfully")
    
    # Place second order
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    second_order_id = response.json()["order_id"]
    print("✓ Second order placed successfully")
    
    # Mark first order as delivered
    response = requests.put(f"{MARKETPLACE_SERVICE}/orders/{first_order_id}", 
                json={"order_id": first_order_id, "status": "DELIVERED"})
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    
    
    # Get initial product stocks
    initial_stocks = {}
    for product_id in [101, 102]:
        response = requests.get(f"{MARKETPLACE_SERVICE}/products/{product_id}")
        initial_stocks[product_id] = response.json()["stock_quantity"]
    
    # Delete user
    response = requests.delete(f"{USER_SERVICE}/users/{user_id}")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    
    # Verify product stocks were restored for non-delivered order
    for product_id in [101, 102]:
        response = requests.get(f"{MARKETPLACE_SERVICE}/products/{product_id}")
        current_stock = response.json()["stock_quantity"]
        # Stock should be restored only for the second (non-delivered) order
        expected_stock = initial_stocks[product_id] + (1 if product_id == 101 else 2)
        assert current_stock == expected_stock, f"Stock not correctly restored for product {product_id}"
    
    # Verify wallet was deleted
    response = requests.get(f"{WALLET_SERVICE}/wallets/{user_id}")
    assert response.status_code == 404, "Wallet should have been deleted"
    
    print("✓ User deletion with active orders handled correctly")

def test_global_reset():
    """Test suite for global system reset"""
    print("\n=== Testing Global System Reset ===")
    
    # Create multiple users with orders
    users = [
        {"id": 3001, "name": "Charlie Brown", "email": "charlie@example.com"},
        {"id": 3002, "name": "Diana Prince", "email": "diana@example.com"}
    ]
    
    created_order_ids = []
    
    for user in users:
        # Create user
        response = requests.post(f"{USER_SERVICE}/users", json=user)
        user_id = response.json()["id"]
        
        # Initialize wallet
        requests.put(f"{WALLET_SERVICE}/wallets/{user_id}", 
                    json={"action": "credit", "amount": 1500000})
        
        # Place order
        order_data = {
            "user_id": user_id,
            "items": [
                {"product_id": 101, "quantity": 1}
            ]
        }
        response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
        created_order_ids.append(response.json()["order_id"])
    
    # Get initial product stock
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/101")
    initial_stock = response.json()["stock_quantity"]
    
    # Test global reset
    print("\nTest 12: Testing global system reset")
    print("Current State of Order: ", requests.get(f"{MARKETPLACE_SERVICE}/orders").json())
    response = requests.delete(f"{USER_SERVICE}/users")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    
    # Verify all users are deleted
    for user in users:
        response = requests.get(f"{USER_SERVICE}/users/{user['id']}")
        assert response.status_code == 404, f"User {user['id']} should have been deleted"
    
    # Verify all wallets are deleted
    for user in users:
        response = requests.get(f"{WALLET_SERVICE}/wallets/{user['id']}")
        assert response.status_code == 404, f"Wallet for user {user['id']} should have been deleted"
    
    # Verify product stock is restored
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/101")
    final_stock = response.json()["stock_quantity"]
    print("Current State of Order: ", requests.get(f"{MARKETPLACE_SERVICE}/orders").json())
    assert final_stock == initial_stock + len(users)+2, "Product stock not properly restored after reset, expected: " + str(initial_stock + len(users) +2) + " got: " + str(final_stock)
    
    # Verify orders are cancelled
    for order_id in created_order_ids:
        response = requests.get(f"{MARKETPLACE_SERVICE}/orders/{order_id}")
        assert response.json()["status"] == "CANCELLED", f"Order {order_id} should be cancelled"
    
    print("✓ Global system reset successful")

def main():
    try:
        # Reset system state
        requests.delete(f"{USER_SERVICE}/users")
        
        # Run test suites
        test_user_operations()
        test_wallet_operations()
        test_order_operations()
        test_complex_deletion_scenarios()
        test_global_reset()
        
        print("\n=== All tests completed successfully! ===")
        
    except AssertionError as e:
        print(f"\n❌ Test failed: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
