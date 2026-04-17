"""
Student Marketplace — FastAPI backend.

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from typing import Optional

from database import init_db, get_db
from models import (
    ItemCreate, ItemOut, PaginatedItems,
    get_item_by_id, create_item,
    get_categories, get_items_paginated,
    get_similar_items, mark_item_sold,
)

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="1.0.0",
)


# ---------------------------------------------------------------------------
# Middleware
# ---------------------------------------------------------------------------

class PoweredByMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Powered-By"] = "FastAPI/0.104.1 Python/3.11"
        return response


app.add_middleware(PoweredByMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ---------------------------------------------------------------------------
# Error handlers
# ---------------------------------------------------------------------------

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """S007: Return 400 with field-level error messages."""
    errors = {}
    for error in exc.errors():
        field = str(error["loc"][-1]) if error["loc"] else "general"
        msg = error["msg"]
        if "greater than 0" in msg or "gt" in msg:
            msg = "Must be greater than 0"
        elif "min_length" in msg or "ensure this value has at least" in msg:
            msg = "This field is required"
        elif "missing" in msg or "field required" in msg:
            msg = "This field is required"
        errors[field] = msg
    return JSONResponse(status_code=400, content={"errors": errors})


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logging.error(f"Unhandled error: {exc}", exc_info=True)
    return JSONResponse(status_code=500, content={"error": "Internal server error"})


# ---------------------------------------------------------------------------
# Startup
# ---------------------------------------------------------------------------

@app.on_event("startup")
def on_startup():
    init_db()


# ---------------------------------------------------------------------------
# Items
# ---------------------------------------------------------------------------

@app.get("/items")
def list_items(
    category: Optional[str] = None,
    sort: Optional[str] = None,
    q: Optional[str] = None,
    page: int = 1,
    limit: int = 10,
):
    """
    S013: filter by category
    S016+S017: sort by price_asc/desc, date_asc/desc, combined with category
    S025: search with q param (LIKE title+description)
    S027: paginated — returns {items, total_count, page, limit}
    """
    return get_items_paginated(category=category, sort=sort, q=q, page=page, limit=limit)


@app.get("/items/search")
def search_items(q: str = "", category: Optional[str] = None, sort: Optional[str] = None):
    """S025: Search endpoint (also available via /items?q=)."""
    return get_items_paginated(q=q, category=category, sort=sort, page=1, limit=50)


@app.get("/items/{item_id}", response_model=ItemOut)
def item_detail(item_id: int):
    """S001: Return a single item by id with all fields."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")
    return item


@app.post("/items", response_model=ItemOut, status_code=201)
def add_item(item: ItemCreate):
    """S006: Create a new marketplace listing. S007: validation errors returned as 400."""
    return create_item(item)


@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
    """S020: PATCH /items/:id — supports {is_sold: true/false} to mark sold/available."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")

    # Handle is_sold specifically via the safe helper
    if "is_sold" in request_body and len(request_body) == 1:
        return mark_item_sold(item_id, bool(request_body["is_sold"]))

    # Generic field update (allowed fields only)
    ALLOWED = {"title", "description", "price", "category", "image_url", "seller_name", "is_sold"}
    fields, values = [], []
    for key, value in request_body.items():
        if key not in ALLOWED:
            raise HTTPException(status_code=400, detail=f"Field not allowed: {key}")
        fields.append(f"{key} = %s")
        values.append(value)

    if not fields:
        raise HTTPException(status_code=400, detail="No valid fields provided")

    values.append(item_id)
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(f"UPDATE items SET {', '.join(fields)} WHERE id = %s", values)
    conn.commit()
    cursor.close()
    conn.close()
    return get_item_by_id(item_id)


@app.delete("/items/{item_id}")
def delete_item(item_id: int):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM items WHERE id = %s", (item_id,))
    conn.commit()
    cursor.close()
    conn.close()
    return {"message": "Item deleted"}


# ---------------------------------------------------------------------------
# Similar items (S033)
# ---------------------------------------------------------------------------

@app.get("/items/{item_id}/similar", response_model=list[ItemOut])
def similar_items(item_id: int):
    """S033: Return up to 5 items in the same category, excluding the current one."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")
    return get_similar_items(item_id=item_id, category=item["category"])


# ---------------------------------------------------------------------------
# Categories (S012)
# ---------------------------------------------------------------------------

@app.get("/categories", response_model=list[str])
def list_categories():
    """S012: Return distinct categories from the database."""
    return get_categories()


# ---------------------------------------------------------------------------
# Sellers
# ---------------------------------------------------------------------------

@app.post("/sellers/register")
def register_seller(data: dict):
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}


# ---------------------------------------------------------------------------
# Admin
# ---------------------------------------------------------------------------

@app.get("/admin", response_class=HTMLResponse)
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
    items = cursor.fetchall()
    cursor.close()
    conn.close()
    html = "<html><head><title>Admin</title></head><body><h1>Marketplace Admin</h1>"
    for item in items:
        html += f"<div><h3>{item['title']}</h3><p>{item['description']}</p></div>"
    html += "</body></html>"
    return HTMLResponse(content=html)
