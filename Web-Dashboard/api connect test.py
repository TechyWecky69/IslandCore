import requests

API = "http://127.0.0.1:8765"
APPLICATION_KEY = "VzSlJ-xWH5g7Ii3LyfH9R1qM_HzbsreAlfzAaJWBu3M"

username = input("Username: ")
password = input("Password: ")

session = requests.Session()

headers = {
    "X-Dashboard-App-Key": APPLICATION_KEY,
    "Content-Type": "application/json"
}

print("\n[1] Testing API health...")
r = session.get(f"{API}/api/health")
print("Status:", r.status_code)
print("Response:", r.text)

print("\n[2] Attempting login...")

r = session.post(
    f"{API}/api/auth/login",
    headers=headers,
    json={
        "username": username,
        "password": password
    }
)

print("Status:", r.status_code)
print("Response:", r.text)

if r.ok:
    print("\n[3] Login succeeded!")

    try:
        data = r.json()
        print("JSON:", data)

        # Try to find a token if the API returns one
        token = data.get("token")

        if token:
            print("\n[4] Testing authenticated request...")

            auth_headers = {
                "X-Dashboard-App-Key": APPLICATION_KEY,
                "Authorization": f"Bearer {token}"
            }

            r = session.get(
                f"{API}/api/status",
                headers=auth_headers
            )

            print("Status:", r.status_code)
            print("Response:", r.text)

    except Exception as e:
        print("Couldn't parse JSON:", e)

else:
    print("\nLogin failed.")