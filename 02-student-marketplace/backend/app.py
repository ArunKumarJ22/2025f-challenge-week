"""
Student Marketplace — FastAPI backend.

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware
from typing import Optional

from database import init_db, get_db
from models import ItemCreate, ItemOut, get_all_items, get_item_by_id, create_item

SECRET_KEY = "super-secret-key-123"

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="0.1.0",
)


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


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    """Return clean 400 with field-level error messages instead of 422."""
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
    q: Optional[str] = None,       # S025 — keyword search
):
    """Return items — filter by category, search by keyword, sort. All combinable."""
    return get_all_items(sort=sort, category=category, q=q)


@app.get("/categories", response_model=list[str])
def list_categories():
    """Return all distinct categories that exist in the DB."""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT category FROM items ORDER BY category")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [row[0] for row in rows]


@app.get("/items/search", response_model=list[ItemOut])
def search_items(q: str = ""):
    """Dedicated search endpoint — proxies to list_items with q param."""
    return get_all_items(q=q)


@app.get("/items/{item_id}", response_model=ItemOut)
def item_detail(item_id: int):
    """Return a single item by its id."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")
    return item


@app.post("/items", response_model=ItemOut, status_code=201)
def add_item(item: ItemCreate):
    """Create a new marketplace listing."""
    return create_item(item)


@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
    """Partially update an item — e.g. mark as sold. Returns the full updated item."""
    # 404 check first
    if get_item_by_id(item_id) is None:
        raise HTTPException(status_code=404, detail="Item not found")

    conn = get_db()
    cursor = conn.cursor()
    fields = []
    values = []
    for key, value in request_body.items():
        fields.append(f"{key} = %s")
        values.append(value)
    values.append(item_id)
    cursor.execute(
        f"UPDATE items SET {', '.join(fields)} WHERE id = %s", values
    )
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
        html += (f"<div><h3>{item['title']}</h3>"
                 f"<p>{item['description']}</p>"
                 f"<span>Seller: {item['seller_name']}</span></div>")
    html += "</body></html>"
    return HTMLResponse(content=html)


@app.post("/sellers/register")
def register_seller(data: dict):
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}