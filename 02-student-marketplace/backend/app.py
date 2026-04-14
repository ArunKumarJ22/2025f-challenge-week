"""
Student Marketplace — FastAPI backend (SECURED VERSION).

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
<<<<<<< HEAD
<<<<<<< HEAD
from fastapi.responses import HTMLResponse, JSONResponse
=======
<<<<<<< HEAD
=======
>>>>>>> 5ebb916 (GET /categories (DISTINCT))
from fastapi.responses import JSONResponse
from fastapi.responses import HTMLResponse
=======
from fastapi.responses import HTMLResponse, JSONResponse
>>>>>>> b376abb (| SMKT-S017 | [API] Combine sort + category filter |)
>>>>>>> 6b48552 (your message)
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional

from database import init_db, get_db
from models import (
    ItemCreate, ItemOut,
    get_all_items, get_item_by_id, create_item,
    get_categories, get_items_filtered,
)

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="0.2.0-secure",
)
<<<<<<< HEAD

<<<<<<< HEAD
# ---------------------------
#  CORS (FIXED)
# ---------------------------
=======
=======
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Convert Pydantic validation errors into a clean 400 with field-level messages."""
    errors = {}
    for error in exc.errors():
        # error["loc"] is e.g. ("body", "price") — we want just "price"
        field = error["loc"][-1] if error["loc"] else "general"
        msg = error["msg"]
        # Make messages friendlier
        if "greater than 0" in msg or "gt" in msg:
            msg = "Must be greater than 0"
        elif "ensure this value has at least" in msg or "min_length" in msg:
            msg = "This field is required"
        elif "field required" in msg or msg == "missing":
            msg = "This field is required"
        errors[str(field)] = msg
    return JSONResponse(status_code=400, content={"errors": errors})

>>>>>>> 5ebb916 (GET /categories (DISTINCT))
class PoweredByMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Powered-By"] = "FastAPI/0.104.1 Python/3.11"
        return response


app.add_middleware(PoweredByMiddleware)

>>>>>>> 6b48552 (your message)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://yourfrontend.com"],  # change to your frontend
    allow_credentials=True,
    allow_methods=["GET", "POST", "PATCH", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)

# ---------------------------
# GLOBAL EXCEPTION HANDLER (CWE-209 FIX)
# ---------------------------
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logging.error(f"Unhandled error: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"error": "Internal server error"},
    )

<<<<<<< HEAD
# ---------------------------
# VALIDATION HANDLER
# ---------------------------
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
=======
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Return clean 400 with field-level error messages instead of 422."""
>>>>>>> 6b48552 (your message)
    errors = {}
    for error in exc.errors():
        field = str(error["loc"][-1]) if error["loc"] else "general"
        msg = error["msg"]
<<<<<<< HEAD
        if "greater than 0" in msg:
=======
        if "greater than 0" in msg or "gt" in msg:
>>>>>>> 6b48552 (your message)
            msg = "Must be greater than 0"
        elif "min_length" in msg or "ensure this value has at least" in msg:
            msg = "This field is required"
        elif "missing" in msg or "field required" in msg:
            msg = "This field is required"
        errors[field] = msg
    return JSONResponse(status_code=400, content={"errors": errors})

<<<<<<< HEAD
# ---------------------------
#  STARTUP
# ---------------------------
=======

>>>>>>> 6b48552 (your message)
@app.on_event("startup")
def on_startup():
    init_db()


# ---------- Endpoints ----------


@app.get("/items", response_model=list[ItemOut])
def list_items():
    """Return all marketplace items, newest first."""
    return get_all_items()


@app.get("/items/{item_id}", response_model=ItemOut)
def item_detail(item_id: int):
    try:
        item = get_item_by_id(item_id)
        if item is None:
            raise HTTPException(status_code=404, detail="Item not found")
        return item
    except HTTPException:
        raise
    except Exception as e:
        logging.error(f"Error fetching item: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch item")


@app.post("/items", response_model=ItemOut, status_code=201)
def add_item(item: ItemCreate):
<<<<<<< HEAD
    try:
        return create_item(item)
    except Exception as e:
        logging.error(f"Error creating item: {e}")
        raise HTTPException(status_code=500, detail="Failed to create item")

ALLOWED_FIELDS = {"title", "description", "price", "category", "sold"}
=======
    """Create a new marketplace listing."""
    return create_item(item)


# ---------- Search ----------

@app.get("/items/search")
def search_items(q: str = ""):
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(f"SELECT * FROM items WHERE title LIKE '%{q}%' OR description LIKE '%{q}%' ORDER BY created_at DESC")
    results = cursor.fetchall()
    cursor.close()
    conn.close()
    return results


# ---------- Admin ----------

@app.get("/admin", response_class=HTMLResponse)
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    html = "<html><head><title>Admin - Items</title></head><body>"
    html += "<h1>Marketplace Admin Panel</h1>"
    for item in items:
        html += f"<div class='item'><h3>{item['title']}</h3><p>{item['description']}</p><span>Seller: {item['seller_name']}</span></div>"
    html += "</body></html>"
    return HTMLResponse(content=html)


# ---------- Delete item (no auth) ----------

@app.delete("/items/{item_id}")
def delete_item(item_id: int):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM items WHERE id = %s", (item_id,))
    conn.commit()
    cursor.close()
    conn.close()
    return {"message": "Item deleted"}


<<<<<<< HEAD
>>>>>>> 6b48552 (your message)

@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
<<<<<<< HEAD
    conn = None
    cursor = None
    try:
        if get_item_by_id(item_id) is None:
            raise HTTPException(status_code=404, detail="Item not found")

<<<<<<< HEAD
        fields = []
        values = []

        for key, value in request_body.items():
            if key not in ALLOWED_FIELDS:
                raise HTTPException(status_code=400, detail=f"Invalid field: {key}")
            fields.append(f"{key} = %s")
            values.append(value)

        if not fields:
            raise HTTPException(status_code=400, detail="No valid fields provided")

        values.append(item_id)

        conn = get_db()
        cursor = conn.cursor()
        cursor.execute(
            f"UPDATE items SET {', '.join(fields)} WHERE id = %s", values
        )
        conn.commit()

        return get_item_by_id(item_id)

    except HTTPException:
        raise
    except Exception as e:
        logging.error(f"Update error: {e}")
        raise HTTPException(status_code=500, detail="Failed to update item")
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


@app.delete("/items/{item_id}")
def delete_item(item_id: int):
    conn = None
    cursor = None
    try:
        conn = get_db()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM items WHERE id = %s", (item_id,))
        conn.commit()
        return {"message": "Item deleted"}
    except Exception as e:
        logging.error(f"Delete failed: {e}")
        raise HTTPException(status_code=500, detail="Failed to delete item")
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


@app.get("/admin", response_class=HTMLResponse)
def admin_page():
    conn = None
    cursor = None
    try:
        conn = get_db()
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
        items = cursor.fetchall()

        html = "<html><head><title>Admin</title></head><body>"
        html += "<h1>Marketplace Admin Panel</h1>"

        for item in items:
            html += (
                f"<div><h3>{item['title']}</h3>"
                f"<p>{item['description']}</p>"
                f"<span>Seller: {item['seller_name']}</span></div>"
            )

        html += "</body></html>"
        return HTMLResponse(content=html)

    except Exception as e:
        logging.error(f"Admin page error: {e}")
        return HTMLResponse(content="Internal server error", status_code=500)

    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()
=======
=======
@app.get("/admin", response_class=HTMLResponse)
def admin_page():
>>>>>>> b376abb (| SMKT-S017 | [API] Combine sort + category filter |)
=======
    """Partially update an item (e.g. mark as sold)."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")

>>>>>>> 5ebb916 (GET /categories (DISTINCT))
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
    items = cursor.fetchall()
    cursor.close()
    conn.close()
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 5ebb916 (GET /categories (DISTINCT))
    # Return the updated item so test_mark_as_sold can assert is_sold == True
    return get_item_by_id(item_id)

=======
    html = "<html><head><title>Admin</title></head><body>"
    html += "<h1>Marketplace Admin Panel</h1>"
    for item in items:
        html += (f"<div><h3>{item['title']}</h3>"
                 f"<p>{item['description']}</p>"
                 f"<span>Seller: {item['seller_name']}</span></div>")
    html += "</body></html>"
    return HTMLResponse(content=html)
>>>>>>> b376abb (| SMKT-S017 | [API] Combine sort + category filter |)
>>>>>>> 6b48552 (your message)


@app.post("/sellers/register")
def register_seller(data: dict):
<<<<<<< HEAD
    try:
        name = data.get("name", "")
        email = data.get("email", "")
        logging.info(f"New seller registered: {name} ({email})")
        return {"message": f"Seller {name} registered"}
    except Exception as e:
        logging.error(f"Registration error: {e}")
        raise HTTPException(status_code=500, detail="Registration failed")
=======
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}
>>>>>>> 6b48552 (your message)
