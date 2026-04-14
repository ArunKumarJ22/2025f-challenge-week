# Threat Analysis Report

**File analysed:** `backend/app.py`  
**Total threats found:** 6 - unmitigated

---

## S - Spoofing Identity

- **Severity:** High
- **Affected endpoints:** `POST /items`, `POST /sellers/register`
- **Status:** Unmitigated
- **Description:**  
  There is no authentication system. Any person can claim to be any seller by simply typing any name into the `seller_name` field. There is no login, no session, no token, and no email verification in the entire application.

---

## T - Tampering with Data

- **Severity:** Critical
- **Affected endpoints:** `PATCH /items/{id}`, `DELETE /items/{id}`, `GET /items/search`
- **Status:** Unmitigated
- **Description:**  
  An attacker can modify or delete any listing without owning it. Two separate SQL injection vulnerabilities also allow direct manipulation of database records.

---

## R - Repudiation

- **Severity:** Medium
- **Affected endpoints:** All mutating endpoints (`POST`, `PATCH`, `DELETE`)
- **Status:** Unmitigated
- **Description:**  
  An attacker can deny having created, modified, or deleted any listing. There is no audit trail, no user identity stored with actions, and no request logging that ties an action to a real person.

---

## I - Information Disclosure

- **Severity:** Critical
- **Affected endpoints:** All endpoints
- **Status:** Unmitigated
- **Description:**  
  The application leaks internal system details, framework versions, database structure, and all user data through multiple channels.

---

## D - Denial of Service

- **Severity:** Medium
- **Affected endpoints:** `POST /items`, `GET /items/search`, `DELETE /items/{id}`
- **Status:** Unmitigated
- **Description:**  
  No rate limiting exists anywhere. An attacker can flood the API with requests or expensive queries, exhausting server resources and making the marketplace unavailable.

---

## E - Elevation of Privilege

- **Severity:** Critical
- **Affected endpoints:** *Not specified in the report*
- **Status:** *Not specified*
- **Description:** *No details provided in the original analysis*

Affected endpoints:GET /items/search, GET /admin, Werkzeug debugger
Status:Unmitigated
Description
An attacker can escalate from anonymous user to full server-level code execution through two separate paths.

