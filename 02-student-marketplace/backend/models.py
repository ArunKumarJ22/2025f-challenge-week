"""
Pydantic models and database query functions for items.
Covers: S001, S006, S007, S012, S013, S016, S017, S020, S025, S027, S033
"""

from pydantic import BaseModel, Field
from typing import Optional, List
from database import get_db


# ---------------------------------------------------------------------------
# Pydantic schemas
# ---------------------------------------------------------------------------

class ItemCreate(BaseModel):
    """S006: Schema used when creating a new item — validates title, price, category."""
    title: str = Field(..., min_length=1, max_length=200)
    description: Optional[str] = None
    price: float = Field(..., gt=0)
    category: str = Field(..., min_length=1)
    image_url: Optional[str] = None
    seller_name: str = Field(..., min_length=1)


class ItemOut(BaseModel):
    """S001: Schema returned to clients — includes all fields."""
    id: int
    title: str
    description: Optional[str]
    price: float
    category: str
    image_url: Optional[str]
    seller_name: str
    created_at: str
    is_sold: bool


class PaginatedItems(BaseModel):
    """S027: Paginated response wrapper."""
    items: List[ItemOut]
    total_count: int
    page: int
    limit: int


# ---------------------------------------------------------------------------
# Sort options (S016)
# ---------------------------------------------------------------------------

SORT_OPTIONS = {
    "price_asc":  "price ASC",
    "price_desc": "price DESC",
    "date_asc":   "created_at ASC",
    "date_desc":  "created_at DESC",
}


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


def get_all_items() -> List[dict]:
    """Return every item, newest first."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute("SELECT * FROM items ORDER BY created_at DESC")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [_serialize_row(r) for r in rows]


def get_item_by_id(item_id: int) -> Optional[dict]:
    """S001: Return a single item or None."""
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
    """S006: Insert a new item and return it (with generated id and timestamp)."""
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
    """S012: Return a sorted list of distinct categories."""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute("SELECT DISTINCT category FROM items ORDER BY category ASC")
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [row[0] for row in rows]

def get_items_filtered(category=None, q=None, sort=None):
    order = SORT_OPTIONS.get(sort, "created_at DESC")

    conditions = []
    params = []

    if category:
        conditions.append("category = %s")
        params.append(category)

    if q:
        conditions.append("(title LIKE %s OR description LIKE %s)")
        params.append(f"%{q}%")
        params.append(f"%{q}%")

    where = ("WHERE " + " AND ".join(conditions)) if conditions else ""

    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    cursor.execute(f"SELECT * FROM items {where} ORDER BY {order}", params)
    rows = cursor.fetchall()

    cursor.close()
    conn.close()

    return [_serialize_row(r) for r in rows]

def get_items_paginated(
    category: Optional[str] = None,
    sort: Optional[str] = None,
    q: Optional[str] = None,
    page: int = 1,
    limit: int = 10,
) -> dict:
    """
    S013: filter by category
    S016 + S017: sort param (price_asc/desc, date_asc/desc) combined with category
    S025: search by q (LIKE title+description)
    S027: pagination — page + limit + total_count
    """
    order = SORT_OPTIONS.get(sort, "created_at DESC")
    offset = (page - 1) * limit

    conditions = []
    params = []

    if category:
        conditions.append("category = %s")
        params.append(category)

    if q:
        conditions.append("(title LIKE %s OR description LIKE %s)")
        params.append(f"%{q}%")
        params.append(f"%{q}%")

    where = ("WHERE " + " AND ".join(conditions)) if conditions else ""

    conn = get_db()
    cursor = conn.cursor(dictionary=True)

    # Total count for pagination header
    cursor.execute(f"SELECT COUNT(*) as cnt FROM items {where}", params)
    total = cursor.fetchone()["cnt"]

    # Paginated results
    cursor.execute(
        f"SELECT * FROM items {where} ORDER BY {order} LIMIT %s OFFSET %s",
        params + [limit, offset]
    )
    rows = cursor.fetchall()
    cursor.close()
    conn.close()

    return {
        "items": [_serialize_row(r) for r in rows],
        "total_count": total,
        "page": page,
        "limit": limit,
    }


def get_similar_items(item_id: int, category: str, limit: int = 5) -> List[dict]:
    """S033: Return up to 5 items in the same category, excluding the current item."""
    conn = get_db()
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        "SELECT * FROM items WHERE category = %s AND id != %s ORDER BY created_at DESC LIMIT %s",
        (category, item_id, limit)
    )
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return [_serialize_row(r) for r in rows]


def mark_item_sold(item_id: int, is_sold: bool) -> Optional[dict]:
    """S020: PATCH is_sold field on an item."""
    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(
        "UPDATE items SET is_sold = %s WHERE id = %s",
        (1 if is_sold else 0, item_id)
    )
    conn.commit()
    cursor.close()
    conn.close()
    return get_item_by_id(item_id)
