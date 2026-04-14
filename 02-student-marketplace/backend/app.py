"""
Student Marketplace — FastAPI backend.

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse
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

# Allow all origins so the Android / iOS emulators can reach the server.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    init_db()


# ---------- Endpoints ----------


@app.get("/items", response_model=list[ItemOut])
def list_items(sort: Optional[str] = None, category: Optional[str] = None):
    """Return all marketplace items, newest first."""
    return get_all_items(sort=sort, category=category)


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



@app.patch("/items/{item_id}")
def update_item(item_id: int, request_body: dict):
    conn = get_db()
    cursor = conn.cursor()
    fields = []
    values = []
    for key, value in request_body.items():
        fields.append(f"{key} = %s")
        values.append(value)
    values.append(item_id)
    cursor.execute(f"UPDATE items SET {', '.join(fields)} WHERE id = %s", values)
    conn.commit()
    cursor.close()
    conn.close()
    return {"message": "Item updated"}



@app.post("/sellers/register")
def register_seller(data: dict):
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}
