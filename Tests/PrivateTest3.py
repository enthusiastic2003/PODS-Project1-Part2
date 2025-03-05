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
    
    # Get initial product stock
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/102")
    initial_stock = response.json()["stock_quantity"]
    initial_price = response.json()["price"]
    
    # Test 6: Place first order (with discount)

    print("\nTest 6: Placing first order (should get discount)")
    order_data = {
        "user_id": 1001,
        "items": [
            {"product_id": 102, "quantity": 1}
        ]
    }
    response = requests.put(f"{WALLET_SERVICE}/wallets/1001", 
                          json={"action": "credit", "amount": 1000000})
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code}, message: {response.text}"
    assert response.json()["total_price"] == int(initial_price * 0.9), f"Discount not applied correctly, expected: {initial_price*0.9}, got: {response.json()["total_price"]}"
    first_order_id = response.json()["order_id"]
    print("✓ First order placed with discount")
    
    # Test 7: Place second order (no discount)
    print("\nTest 7: Placing second order (should not get discount)")
    response = requests.post(f"{MARKETPLACE_SERVICE}/orders", json=order_data)
    assert response.status_code == 201, f"Expected 201, got {response.status_code}"
    assert response.json()["total_price"] == initial_price, "Incorrect price for second order, "
    print("✓ Second order placed without discount")
    
    # Test 8: Check stock reduction
    print("\nTest 8: Verifying stock reduction")
    response = requests.get(f"{MARKETPLACE_SERVICE}/products/102")
    assert response.json()["stock_quantity"] == initial_stock - 2, f"Stock not reduced correctly, expected: {initial_stock-2}, got: {response.json()['stock_quantity']}"
    print("✓ Stock reduced correctly")
    
    # Test 9: Cancel order and verify refund
    print("\nTest 9: Testing order cancellation and refund")
    wallet_before = requests.get(f"{WALLET_SERVICE}/wallets/1001").json()["balance"]
    response = requests.delete(f"{MARKETPLACE_SERVICE}/orders/{first_order_id}")
    assert response.status_code == 200, f"Expected 200, got {response.status_code}"
    
    # Verify wallet refund
    wallet_after = requests.get(f"{WALLET_SERVICE}/wallets/1001").json()["balance"]
    assert wallet_after == wallet_before + int(initial_price * 0.9), "Refund amount incorrect"
    print("✓ Order cancelled and refund processed")
    
    # Test 10: Try to cancel already cancelled order
    print("\nTest 10: Attempting to cancel already cancelled order")
    response = requests.delete(f"{MARKETPLACE_SERVICE}/orders/{first_order_id}")
    assert response.status_code == 400, f"Expected 400, got {response.status_code}"
    print("✓ Double cancellation prevented")

def main():
    try:
        # Reset system state
        requests.delete(f"{USER_SERVICE}/users")
        
        # Run test suites
        test_user_operations()
        test_wallet_operations()
        test_order_operations()
        
        print("\n=== All tests completed successfully! ===")
        
    except AssertionError as e:
        print(f"\n❌ Test failed: {str(e)}")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Unexpected error: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()
