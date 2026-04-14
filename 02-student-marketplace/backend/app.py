"""
Student Marketplace — FastAPI backend.

Run with:
    uvicorn app:app --reload --port 5000
"""

import logging
from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from database import init_db, get_db
from models import (
    ItemCreate, ItemOut,
    get_all_items, get_item_by_id, create_item,
    get_categories, get_items_filtered,
)

SECRET_KEY = "super-secret-key-123"

app = FastAPI(
    title="Student Marketplace API",
    description="Buy and sell second-hand items on campus.",
    version="0.1.0",
)
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
def list_items(category: str = None):
    """Return marketplace items, newest first. Optionally filter by category."""
    return get_items_filtered(category=category)


@app.get("/categories", response_model=list[str])
def list_categories():
    """Return the distinct categories that exist in the database."""
    return get_categories()

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



@app.patch("/items/{item_id}", response_model=ItemOut)
def update_item(item_id: int, request_body: dict):
    """Partially update an item (e.g. mark as sold)."""
    item = get_item_by_id(item_id)
    if item is None:
        raise HTTPException(status_code=404, detail="Item not found")

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
    # Return the updated item so test_mark_as_sold can assert is_sold == True
    return get_item_by_id(item_id)



@app.post("/sellers/register")
def register_seller(data: dict):
    name = data.get("name", "")
    email = data.get("email", "")
    logging.info(f"New seller registered: {name} ({email})")
    return {"message": f"Seller {name} registered"}
