 Attack Tree – Student Marketplace

## Attack Tree 1: SQL Injection Extract all data
Attacker Goal: Read all items without permission
└── OR
    ├── Use public /items endpoint
    │     └── Backend returns all rows by design
    └── Exploit SQL injection in /items/search
          ├── Inject payload: ' OR '1'='1
          └── Database query returns all rows

## Attack Tree 2: IDOR & Missing Auth to Delete any item
Attacker Goal: Delete another user's item
└── AND
    ├── Find item ID
    │     └── IDs are public from GET /items
    └── Call DELETE /items/{id}
          ├── No authentication
          ├── No authorization / ownership check
          └── Item is deleted

## Attack Tree 3: CORS misconfig & XSS Steal admin session
Attacker Goal: Steal admin session cookie
└── AND
    ├── Inject XSS payload via item title
    │     └── Payload stored in DB and rendered in /admin
    └── CORS allows any origin with credentials
          └── Attacker’s site can read authenticated responses
          AND
        ├── Admin visits /admin
        └── Malicious script exfiltrates session cookie
