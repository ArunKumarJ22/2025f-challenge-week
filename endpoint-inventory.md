 Endpoint Inventory
Total Endpoints: 10
Authenticated: 0 / 10
Severity Count 
 Critical - 4 ,High - 1 ,Medium - 1 ,Low -4 
EP1 - GET /items/search
Method - GET 
Path- /items/search
Input - search string - user controlled
Severity - Critical 
 CWE-89 (SQL Injection) 
Risk:The  parameter is interpolated directly into a raw SQL f-string with no sanitisation. An attacker can break out of the clause and run arbitrary SQL.
EP-2 - POST /items
Method - POST 
Path - /items
Input - title, description, price, category, seller_name (JSON body) 
Severity - Critical 
CWE-79 (Stored XSS), CWE-306 (Missing Auth) 
Risk: title and description are stored in DB and rendered unescaped in /admin HTML page. Any attacker can post a listing with a script payload. seller_name is free text - anyone can impersonate any user. No rate limiting allows flooding.
 EP-3 - PATCH /items/{id}
Method - PATCH 
Path - /items/{id}
Input - Any JSON key+value in body; id in path 
Severity - Critical 
CWE-639 (IDOR), CWE-89 (SQL Injection) |
Risk - No ownership check  any user can modify any listing. The PATCH handler builds SQL `SET` clauses using raw request body keys as column names. An attacker controls both the column name and value.
 EP-4 — GET /admin
Method -  GET 
Path - /admin
Input -  None
Severity - Critical 
CWE-306 (Missing Auth), CWE-79 (XSS) 
Risk: Admin panel is publicly accessible with no login. Renders all item data as raw HTML without escaping  XSS payloads posted via POST /items execute here in any browser that visits this page.
EP-5 - DELETE /items/{id}
Method - DELETE 
Path - /items/{id}
Input - id 
Severity - High 
CWE-639 (IDOR)
Risk: No auth, no ownership check. Any anonymous user can delete any listing by iterating integer IDs. A simple loop can wipe the entire marketplace in seconds.
 EP-06 - POST /sellers/register
Method - POST 
Path - /sellers/register
Input - name, email (JSON body) 
Severity - Medium 
 CWE-532 (PII in Logs) 
Risk: email is written directly to application logs via logging.info(). Logs may be stored insecurely or forwarded to third-party services.
 EP-7 - GET /items
Method - GET 
Path - /items
Input - category, sort, page, limit
Severity - Low 
Risk: Query params passed to DB - potential SQLi via unsanitised  sort and category values. Low severity as read-only.
 EP-8 - GET /items/{id}
Method - GET 
Path - /items/{id}
Input - id (path param - integer) 
Severity -Low 
Risk:  404 response confirms item existence and aids IDOR enumeration. Integer IDs are easily guessable.
 EP-9 - GET /categories
Method -  GET 
Path - /categories
Input - None 
Severity  - Low 
Risk: Read-only. Returns distinct category values. Minimal risk.
 EP-10  - GET /items/{id}/similar
Method - GET 
Path - /items/{id}/similar 
Input - id (path param) 
Severity - Low 
Risk:  Filters by category potential SQLi if category value is unsanitised in query.
