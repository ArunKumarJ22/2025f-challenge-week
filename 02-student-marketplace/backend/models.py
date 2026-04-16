"""
Pydantic models and database query functions for items.
"""

from pydantic import BaseModel, Field
from typing import Optional, List
from database import get_db


# ---------------------------------------------------------------------------
# Pydantic schemas
# ---------------------------------------------------------------------------

class ItemCreate(BaseModel):
    """Schema used when creating a new item (POST body)."""
    title: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = None
    price: float = Field(..., gt=0)
    category: str = Field(..., min_length=1)
    image_url: Optional[str] = None
    seller_name: str = Field(..., min_length=1)


class ItemOut(BaseModel):
    """Schema returned to clients."""
    id: int
    title: str
    description: Optional[str]
    price: float
    category: str
    image_url: Optional[str]
    seller_name: str
    created_at: str
    is_sold: bool


# ---------------------------------------------------------------------------
# DB helpers
# ---------------------------------------------------------------------------

def _serialize_row(row) -> dict:
    """Convert a row dict, handling datetime and is_sold conversion."""
    if row is None:
        return None
    d = dict(row)
    d["is_sold"] = bool(d.get("is_sold", 0))
    for key, value in d.items():
        if hasattr(value, 'isoformat'):
            d[key] = value.isoformat()
    return d

SORT_MAP = {
    "price_asc":  "price ASC",
    "price_desc": "price DESC",
    "date_asc":   "created_at ASC",
    "date_desc":  "created_at DESC",
}

def get_all_items(category: str = None, sort: str = None, q: str = None) -> List[dict]:
    """Return items, optionally filtered by category and/or sorted."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    where = "WHERE 1=1"
params = []

if category:
    where += " AND category = %s"
    params.append(category)

# SAFE SEARCH FIX (only addition)
if q:
    where += " AND title LIKE %s"
    params.append(f"%{q}%")

    # Build ORDER BY clause — use whitelist, default to newest first
    order = SORT_MAP.get(sort, "created_at DESC")

    cursor.execute(
        f"SELECT * FROM items {where} ORDER BY {order}",
        params
    )
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [_serialize_row(r) for r in rows]

def get_item_by_id(item_id: int) -> Optional[dict]:
    """Return a single item or None."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items WHERE id = %s", (item_id,))
    row = cursor.fetchone()
    cursor.close()
    conn.close()
    if row is None:
        return None
    return _serialize_row(row)


def create_item(item: ItemCreate) -> dict:
    """Insert a new item and return it (with generated id and timestamp)."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        """
        INSERT INTO items (title, description, price, category, image_url, seller_name)
        VALUES (%s, %s, %s, %s, %s, %s)
        """,
        (
            item.title,
            item.description,
            item.price,
            item.category,
            item.image_url,
            item.seller_name,
        ),
        
    )
    conn.commit()
    new_id = cursor.lastrowid
    cursor.execute("SELECT * FROM items WHERE id = %s", (new_id,))
    row = cursor.fetchone()
    cursor.close()
    conn.close()
    return _serialize_row(row)

def get_categories() -> List[str]:
    """Return a sorted list of distinct categories that exist in the DB."""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT category FROM items ORDER BY category ASC")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    # Each row is a tuple like ("Books",) — extract the first element
    return [row[0] for row in rows]


def get_items_filtered(category: str = None) -> List[dict]:
    """Return items, optionally filtered by category, newest first."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    if category:
        cursor.execute(
            "SELECT * FROM items WHERE category = %s ORDER BY created_at DESC",
            (category,)
        )
    else:
        cursor.execute("SELECT * FROM items ORDER BY created_at DESC")

    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [_serialize_row(r) for r in rows]



