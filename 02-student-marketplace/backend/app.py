"""
Student Marketplace — FastAPI backend (SECURED VERSION).

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import Optional

from database import init_db, get_db
from models import ItemCreate, ItemOut, get_all_items, get_item_by_id, create_item

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="0.2.0-secure",
)

# ---------------------------
#  CORS (FIXED)
# ---------------------------
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

# ---------------------------
# VALIDATION HANDLER
# ---------------------------
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    errors = {}
    for error in exc.errors():
        field = str(error["loc"][-1]) if error["loc"] else "general"
        msg = error["msg"]
        if "greater than 0" in msg:
            msg = "Must be greater than 0"
        elif "min_length" in msg or "ensure this value has at least" in msg:
            msg = "This field is required"
        elif "missing" in msg or "field required" in msg:
            msg = "This field is required"
        errors[field] = msg
    return JSONResponse(status_code=400, content={"errors": errors})

# ---------------------------
#  STARTUP
# ---------------------------
@app.on_event("startup")
def on_startup():
    init_db()

# ---------------------------
#  ROUTES
# ---------------------------

@app.get("/items", response_model=list[ItemOut])
def list_items(
    sort: Optional[str] = None,
    category: Optional[str] = None,
    q: Optional[str] = None,
):
    try:
        return get_all_items(sort=sort, category=category, q=q)
    except Exception as e:
        logging.error(f"Error fetching items: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch items")


@app.get("/categories", response_model=list[str])
def list_categories():
    conn = None
    cursor = None
    try:
        conn = get_db()
        cursor = conn.cursor()
        cursor.execute("SELECT DISTINCT category FROM items ORDER BY category")
        rows = cursor.fetchall()
        return [row[0] for row in rows]
    except Exception as e:
        logging.error(f"Error fetching categories: {e}")
        raise HTTPException(status_code=500, detail="Failed to fetch categories")
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


@app.get("/items/search", response_model=list[ItemOut])
def search_items(q: str = ""):
    try:
        return get_all_items(q=q)
    except Exception as e:
        logging.error(f"Search error: {e}")
        raise HTTPException(status_code=500, detail="Search failed")


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
    try:
        return create_item(item)
    except Exception as e:
        logging.error(f"Error creating item: {e}")
        raise HTTPException(status_code=500, detail="Failed to create item")

ALLOWED_FIELDS = {"title", "description", "price", "category", "sold"}

@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
    conn = None
    cursor = None
    try:
        if get_item_by_id(item_id) is None:
            raise HTTPException(status_code=404, detail="Item not found")

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


@app.post("/sellers/register")
def register_seller(data: dict):
    try:
        name = data.get("name", "")
        email = data.get("email", "")
        logging.info(f"New seller registered: {name} ({email})")
        return {"message": f"Seller {name} registered"}
    except Exception as e:
        logging.error(f"Registration error: {e}")
        raise HTTPException(status_code=500, detail="Registration failed")