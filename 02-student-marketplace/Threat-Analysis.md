# 1‑Page Threat Assessment – Top 5 Risks

## 1. SQL Injection – `/items/search`
**Likelihood:** High (trivial to exploit)  
**Impact:** Critical — full data breach or data loss  
**Mitigation:** Use parameterised queries; avoid string‑built SQL

---

## 2. Missing Authentication – POST, PATCH, DELETE, `/admin`
**Likelihood:** High (anyone can call these endpoints)  
**Impact:** High — spam creation, tampering, item deletion, admin exposure  
**Mitigation:** Implement JWT / OAuth2 authentication and enforce it on all sensitive routes

---

## 3. IDOR (Insecure Direct Object Reference)
**Issue:** No ownership checks on item modification/deletion  
**Likelihood:** High (any item ID works)  
**Impact:** High — attackers can modify or delete others’ items  
**Mitigation:** Validate `item.seller_id == current_user.id` before allowing updates or deletion

---

## 4. CORS Misconfiguration
**Issue:** `allow_origins=["*"]` combined with `allow_credentials=True`  
**Likelihood:** Medium  
**Impact:** Medium — CSRF‑like attacks, cross‑origin data leakage  
**Mitigation:** Restrict CORS to specific trusted origins; never use `"*"` with credentials

---

## 5. Stored XSS – Admin Panel
**Issue:** User‑controlled item titles rendered in `/admin`  
**Likelihood:** Medium (requires creating an item)  
**Impact:** Medium — session hijacking, UI defacement  
**Mitigation:** Escape HTML output using Jinja2 autoescape or `html.escape()`

---
 
