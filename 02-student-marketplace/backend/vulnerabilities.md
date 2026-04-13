Vulnerability Report: database.py

File Analyzed
database.py – Database helper for Student Marketplace.

Vulnerabilities Found

1. Hardcoded Default Password (Critical)
- Line: 11
- Code: "password": os.environ.get("DB_PASSWORD", "Arun22")
- Issue: If DB_PASSWORD environment variable is not set, the code falls back to "Arun22".
- Impact: Anyone with source code access can see the database password.

2. Duplicate Dictionary Key (Medium)
- Lines: 10 and 11
- Code:
  python
  "password": os.environ.get("DB_PASSWORD", ""),
  "password": os.environ.get("DB_PASSWORD", "Arun22").
