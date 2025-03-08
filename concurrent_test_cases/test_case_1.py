import sys
import requests
import random
from threading import Thread

from user import post_user
from wallet import put_wallet, get_wallet, test_get_wallet
from utils import check_response_status_code, print_fail_message, print_pass_message

WALLET_SERVICE_URL = "http://localhost:8082"

# Global counters
credited_amount = 0
debited_amount = 0

def get_current_balance(user_id):
    resp = get_wallet(user_id)
    if resp.status_code == 200:
        balance = resp.json().get("balance", "Unknown")
        print(f"[BALANCE] Current balance for user {user_id}: {balance}")
        return balance
    else:
        print(f"[BALANCE FAIL] Could not fetch balance for user {user_id}. Response: {resp.status_code}")
        return None

def credit_thread_function(user_id, iterations=50):
    """
    Each thread calls credit random(10..100).
    If response=200, we increment credited_amount.
    """
    global credited_amount
    for _ in range(iterations):
        amount = random.randint(10, 100)
        resp = requests.put(f"{WALLET_SERVICE_URL}/wallets/{user_id}",
                            json={"action": "credit", "amount": amount})
        if resp.status_code == 200:
            credited_amount += amount
            print(f"[CREDIT] User {user_id} credited with {amount}. Total credited: {credited_amount}")
            get_current_balance(user_id)
        else:
            print(f"[CREDIT FAIL] User {user_id} credit of {amount} failed. Response: {resp.status_code}")

def debit_thread_function(user_id, iterations=50):
    """
    Each thread calls debit random(5..50).
    If response=200, we increment debited_amount.
    """
    global debited_amount
    for _ in range(iterations):
        amount = random.randint(5, 50)
        resp = requests.put(f"{WALLET_SERVICE_URL}/wallets/{user_id}",
                            json={"action": "debit", "amount": amount})
        if resp.status_code == 200:
            debited_amount += amount
            print(f"[DEBIT] User {user_id} debited with {amount}. Total debited: {debited_amount}")
            get_current_balance(user_id)
        else:
            print(f"[DEBIT FAIL] User {user_id} debit of {amount} failed. Response: {resp.status_code}")

def main():
    try:
        # Create a user
        user_id = 1001
        print(f"[INFO] Creating user {user_id}...")
        resp = post_user(user_id, "Alice Concurrency", "alice@concurrency.com")
        if not check_response_status_code(resp, 201):
            return False
        print(f"[SUCCESS] User {user_id} created.")

        # Give initial wallet balance
        initial_balance = 1000
        print(f"[INFO] Crediting initial balance of {initial_balance} to user {user_id}...")
        resp = put_wallet(user_id, "credit", initial_balance)
        if not check_response_status_code(resp, 200):
            return False
        print(f"[SUCCESS] Initial balance credited.")
        get_current_balance(user_id)

        # Spawn concurrency threads
        print("[INFO] Starting credit and debit threads...")
        thread1 = Thread(target=credit_thread_function, kwargs={"user_id": user_id, "iterations": 100})
        thread2 = Thread(target=debit_thread_function, kwargs={"user_id": user_id, "iterations": 100})

        thread1.start()
        thread2.start()

        thread1.join()
        thread2.join()

        print("[INFO] All transactions completed.")
        get_current_balance(user_id)

        # Final check of the wallet
        final_expected_balance = initial_balance + credited_amount - debited_amount
        print(f"[INFO] Fetching final wallet balance for user {user_id}...")
        resp = get_wallet(user_id)
        if not test_get_wallet(user_id, resp, final_expected_balance):
            print(f"[FAIL] Final balance mismatch. Expected: {final_expected_balance}")
            return False

        print(f"[SUCCESS] Wallet concurrency test passed. Final balance: {final_expected_balance}")
        return True

    except Exception as e:
        print_fail_message(f"[ERROR] Test crashed with exception: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
