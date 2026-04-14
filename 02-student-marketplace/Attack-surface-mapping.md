# Security Analysis: Campus Marketplace

## 1. Attack Surface Mapping – All Untrusted Inputs

| Input Location | Endpoint / Parameter | Type | Trust Boundary | Vulnerability |
|----------------|----------------------|------|----------------|---------------|
| Query string | `GET /items/search?q=` | User‑controlled string | Mobile → Backend | **SQL injection** (direct concatenation) |
| Path parameter | `GET /items/{item_id}` | Integer (user‑supplied) | Mobile → Backend | Missing validation (negative, zero, large) |
| Path parameter | `DELETE /items/{item_id}` | Integer | Mobile → Backend | No auth, no ownership check |
| Path parameter | `PATCH /items/{item_id}` | Integer | Mobile → Backend | No auth, no validation |
| Request body (JSON) | `POST /items` | `title`, `description`, `price`, `seller_name` | Mobile → Backend | No input validation (XSS, long strings, negative price) |
| Request body (JSON) | `PATCH /items/{item_id}` | Arbitrary JSON keys/values | Mobile → Backend | **Arbitrary field update** (could modify `id`, `created_at`, etc.) |
| Request body (JSON) | `POST /sellers/register` | `name`, `email` | Mobile → Backend | No validation, only logging (no actual registration) |
| HTTP headers | All endpoints | `Origin`, `User-Agent`, etc. | Mobile → Backend | CORS allows `*` – potential CSRF‑like attacks |
| Environment variables | Database config | `DB_PASSWORD`, etc. | Backend → OS | Hardcoded fallback `"Arun22"` (exposed in code) |

---


