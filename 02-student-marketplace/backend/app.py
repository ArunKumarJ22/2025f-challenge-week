"""
Student Marketplace — FastAPI backend (SECURED VERSION).
"""

import logging
from typing import Optional
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from database import init_db, get_db
from models import ItemCreate, ItemOut, get_all_items, get_item_by_id, create_item

logging.basicConfig(level=logging.INFO)

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="0.2.0-secure",
)

class PoweredByMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Powered-By"] = "FastAPI"
        return response

app.add_middleware(PoweredByMiddleware)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # change in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------
# Exception Handlers
# ---------------------------
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logging.error(f"Unhandled error: {exc}", exc_info=True)
    return JSONResponse(status_code=500, content={"error": "Internal server error"})


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
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


@app.on_event("startup")
def on_startup():
    init_db()

@app.get("/items", response_model=list[ItemOut])
def list_items(
    sort: Optional[str] = None,
    category: Optional[str] = None,
    q: Optional[str] = None,
):
    return get_all_items(sort=sort, category=category, q=q)


@app.get("/categories", response_model=list[str])
def list_categories():
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT category FROM items ORDER BY category")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [row[0] for row in rows]


@app.get("/items/{item_id}", response_model=ItemOut)
def item_detail(item_id: int):
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")
    return item


@app.post("/items", response_model=ItemOut, status_code=201)
def add_item(item: ItemCreate):
    return create_item(item)


# ---------------------------
# Update Item (SAFE)
# ---------------------------
ALLOWED_FIELDS = {"title", "description", "price", "category", "is_sold"}

@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
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
    cursor.close()
    conn.close()

    return get_item_by_id(item_id)


# ---------------------------
# Delete
# ---------------------------
@app.delete("/items/{item_id}")
def delete_item(item_id: int):
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM items WHERE id = %s", (item_id,))
    conn.commit()
    cursor.close()
    conn.close()
    return {"message": "Item deleted"}


# ---------------------------
# Admin Page
# ---------------------------
@app.get("/admin", response_class=HTMLResponse)
def admin_page():
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
    items = cursor.fetchall()
    cursor.close()
    conn.close()

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


# ---------------------------
# Seller Registration
# ---------------------------
@app.post("/sellers/register")
def register_seller(data: dict):
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}