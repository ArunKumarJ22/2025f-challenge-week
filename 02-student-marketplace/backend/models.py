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

SORT_OPTIONS = {
    "price_asc":  "price ASC",
    "price_desc": "price DESC",
    "date_asc":   "created_at ASC",
    "date_desc":  "created_at DESC",
}

def get_all_items(sort: Optional[str] = None, category: Optional[str] = None) -> List[dict]:
    """Return every item, newest first."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    order = SORT_OPTIONS.get(sort, "created_at DESC")
    if category:
        cursor.execute(f"SELECT * FROM items WHERE category = %s ORDER BY {order}", (category,))
    else:
        cursor.execute(f"SELECT * FROM items ORDER BY {order}")
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
