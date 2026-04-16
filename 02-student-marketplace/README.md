# Student Marketplace

A campus marketplace where students can buy and sell second-hand items. Built with a **Python/FastAPI** backend and a native mobile client (**Android/Java** or **iOS/Swift**).

This project is approximately **60% complete**. Your challenge: finish the remaining features during the build week.

---

## Architecture

```
┌──────────────┐         HTTP/JSON         ┌──────────────────┐
│              │  GET  /items              │                  │
│   Android    │  GET  /items/{id}        │   FastAPI        │
│     or       │◄─────────────────────────►│   Backend        │
│    iOS       │  POST /items             │   (Python)       │
│   Client     │  PATCH /items/{id} [TODO]│                  │
│              │                           │   MySQL DB       │
└──────────────┘                           └──────────────────┘
```

### What works now

- **Backend:** `GET /items`, `GET /items/{id}`, `POST /items` are fully functional.
- **Database:** MySQL with a seed script (20 realistic items across 4 categories).
- **Android:** Main screen displays the item list from the API. Navigation wired to detail and create screens.
- **iOS:** Table view displays items. Navigation wired to detail and create screens.

### What you need to build

See the [TODOs](#todos) section below.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Python | 3.10+ |
| pip | latest |
| MySQL | 8.0+ |
| Android Studio | 2023.1+ (for Android track) |
| Xcode | 15+ (for iOS track) |

---

## Setup

### Backend

```bash
cd backend

# Make sure MySQL is running and accessible (default: root@localhost:3306)

# Create a virtual environment (recommended)
python -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Seed the database with sample data (creates the DB and table automatically)
python seed.py

# Start the server
uvicorn app:app --reload --port 5000
```

The API is now available at `http://localhost:5000`.

### Android

1. Open the `android/` folder in **Android Studio**.
2. Let Gradle sync.
3. Make sure the backend is running on port 5000.
4. Run on an emulator (the app uses `10.0.2.2:5000` to reach host localhost).

### iOS

See [`ios/README-iOS.md`](ios/README-iOS.md) for detailed Xcode setup instructions.

---

## API Documentation

### `GET /items`

Returns all items, newest first.

```json
[
  {
    "id": 1,
    "title": "Introduction to Algorithms (Cormen, 4th ed.)",
    "description": "Hardcover, some highlighting in chapters 1-12.",
    "price": 35.0,
    "category": "Books",
    "image_url": null,
    "seller_name": "Alice Martin",
    "created_at": "2026-03-28 10:00:00",
    "is_sold": false
  }
]
```

### `GET /items/{id}`

Returns a single item. **404** if not found.

### `POST /items`

Create a new listing.

**Request body:**
```json
{
  "title": "My Item",
  "description": "Optional description",
  "price": 25.00,
  "category": "Books",
  "seller_name": "Your Name"
}
```

**Response:** `201 Created` with the full item object.

---

## Running Tests

```bash
cd backend
pytest tests/ -v
```

You should see **4 passing** and **3 failing** tests. The failing tests correspond to features you need to implement.

---

## TODOs

Each TODO includes **acceptance criteria** so you know when it is done.

### TODO 1 — Item Detail Screen

> **Goal:** Tapping an item in the list opens a detail view with all item info.

**Acceptance criteria:**
- Detail screen loads data from `GET /items/{id}`.
- Displays: title, description, price, category, seller name, posted date.
- If the item is sold, a visible "SOLD" badge is shown.
- Back navigation returns to the list.

**Files to edit:**
- Android: `ItemDetailActivity.java`, `activity_item_detail.xml`
- iOS: `ItemDetailViewController.swift`

---

### TODO 2 — Create Item Form + Validation

> **Goal:** The "+" button opens a form that lets the user post a new item.

**Acceptance criteria:**
- Form fields: title, description, price, category, seller name.
- Validation: title, price, category, and seller name are required. Price must be > 0.
- On submit, `POST /items` is called. On success, navigate back to the list. On error, display an error message.
- The new item appears in the list after returning.

**Files to edit:**
- Android: `CreateItemActivity.java`
- iOS: `CreateItemViewController.swift`

---

### TODO 3 — Filter by Category

> **Goal:** Users can filter the item list to show only one category.

**Acceptance criteria:**
- `GET /items?category=Books` returns only items where `category == "Books"`.
- The mobile app provides a UI control (dropdown, tab bar, or chips) to select a category.
- Selecting "All" (or equivalent) shows all items.
- Test `test_filter_by_category` passes.

**Files to edit:**
- Backend: `app.py`, `models.py`
- Android: `MainActivity.java`
- iOS: `ItemListViewController.swift`

---

### TODO 4 — Sort by Price / Date

> **Goal:** Users can sort items by price or date.

**Acceptance criteria:**
- `GET /items?sort=price_asc` returns items sorted by price ascending.
- Supported values: `price_asc`, `price_desc`, `date_asc`, `date_desc`.
- The mobile app provides a sort toggle or dropdown.
- Test `test_sort_items` passes.

**Files to edit:**
- Backend: `app.py`, `models.py`
- Android: `MainActivity.java`
- iOS: `ItemListViewController.swift`

---

### TODO 5 — Mark as Sold

> **Goal:** A seller can mark their item as sold.

**Acceptance criteria:**
- `PATCH /items/{id}` accepts `{"is_sold": true}` and updates the item.
- The detail screen shows a "Mark as sold" button (hidden if already sold).
- After marking as sold, the item appears dimmed in the list with a "SOLD" badge.
- Test `test_mark_as_sold` passes.

**Files to edit:**
- Backend: `app.py`, `models.py`
- Android: `ItemDetailActivity.java`, `activity_item_detail.xml`
- iOS: `ItemDetailViewController.swift`

---

### TODO 6 (Bonus) — Full-Text Search

> **Goal:** Users can search items by keyword.

**Acceptance criteria:**
- `GET /items?q=keyboard` returns items whose title or description contain "keyboard" (case-insensitive).
- The mobile app has a search bar at the top of the item list.
- Results update as the user types (or on submit).

**Files to edit:**
- Backend: `app.py`, `models.py`
- Android: `MainActivity.java`
- iOS: `ItemListViewController.swift`

---

## Project Structure

```
02-student-marketplace/
├── backend/
│   ├── app.py              # FastAPI application
│   ├── database.py         # MySQL connection helpers
│   ├── models.py           # Pydantic schemas + DB queries
│   ├── seed.py             # Seed 20 sample items
│   ├── requirements.txt    # Python dependencies
│   └── tests/
│       ├── conftest.py     # Test fixtures (test MySQL DB, TestClient)
│       └── test_items.py   # 7 tests (4 pass, 3 fail = your TODOs)
├── android/
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/epita/marketplace/
│   │       │   ├── MainActivity.java
│   │       │   ├── ItemDetailActivity.java      # TODO
│   │       │   ├── CreateItemActivity.java      # TODO
│   │       │   ├── model/Item.java
│   │       │   ├── adapter/ItemAdapter.java
│   │       │   └── api/ApiClient.java
│   │       ├── res/layout/
│   │       │   ├── activity_main.xml
│   │       │   ├── activity_item_detail.xml
│   │       │   ├── activity_create_item.xml
│   │       │   └── item_card.xml
│   │       └── AndroidManifest.xml
│   ├── app/build.gradle
│   ├── build.gradle
│   └── settings.gradle
├── ios/
│   ├── StudentMarketplace/
│   │   ├── Models/Item.swift
│   │   ├── Services/APIClient.swift
│   │   └── Views/
│   │       ├── ItemListViewController.swift
│   │       ├── ItemDetailViewController.swift   # TODO
│   │       └── CreateItemViewController.swift   # TODO
│   ├── StudentMarketplace/Info.plist
│   └── README-iOS.md
├── .gitignore
└── README.md
```


---

## SE TODOs (40)


### Core (01-15)

| Code | TODO |
|------|------|
| SMKT-S001 | [API] GET /items/:id returns all fields |c    
| SMKT-S002 | [Mobile] Detail screen: call API, populate fields |
| SMKT-S003 | [Mobile] Format price + relative date |
| SMKT-S004 | [Mobile] SOLD overlay if is_sold=true |
| SMKT-S005 | [Mobile] Loading spinner, 404 handling |
| SMKT-S006 | [API] POST /items validation (title, price, category) |
| SMKT-S007 | [API] Return 400 with field errors |
| SMKT-S008 | [Mobile] Creation form: title, desc, price, category |
| SMKT-S009 | [Mobile] Category dropdown (Books, Electronics, etc.) |
| SMKT-S010 | [Mobile] Client validation with inline errors |
| SMKT-S011 | [Mobile] On 201, navigate back, item at top |
| SMKT-S012 | [API] GET /categories (DISTINCT) |
| SMKT-S013 | [API] Filter GET /items by category param |
| SMKT-S014 | [Mobile] Horizontal chip row from GET /categories |
| SMKT-S015 | [Mobile] Chip filtering, highlight active, empty state |

### Intermediate (16-25)

| Code | TODO |
|------|------|
| SMKT-S016 | [API] Sort param: price_asc/desc, date_asc/desc |
| SMKT-S017 | [API] Combine sort + category filter |
| SMKT-S018 | [Mobile] Sort button + bottom sheet |
| SMKT-S019 | [Mobile] Persist sort preference |
| SMKT-S020 | [API] PATCH /items/:id {is_sold} |
| SMKT-S021 | [Mobile] 'Mark as Sold' button on detail |
| SMKT-S022 | [Mobile] Confirmation dialog + SOLD overlay |
| SMKT-S023 | [Mobile] List refreshes on back |
| SMKT-S024 | [Mobile] Un-mark: 'Mark as Available' |
| SMKT-S025 | [API] Search param (LIKE title+desc) |

### Advanced (26-35)

| Code | TODO |
|------|------|
| SMKT-S026 | [Mobile] Search bar with debounce + clear |
| SMKT-S027 | [API] Pagination: page + limit + total_count |
| SMKT-S028 | [Mobile] Infinite scroll |
| SMKT-S029 | [Mobile] Pull-to-refresh |
| SMKT-S030 | [Mobile] List cards: image, title, price, badge, status |
| SMKT-S031 | [Mobile] 'Contact Seller' email button |
| SMKT-S032 | [Mobile] Share button on detail |
| SMKT-S033 | [API] GET /items/:id/similar (same category, limit 5) |
| SMKT-S034 | [Mobile] Similar Items section on detail |
| SMKT-S035 | [Mobile] Similar item tap navigates to detail |

### Polish (36-40)

| Code | TODO |
|------|------|
| SMKT-S036 | [Mobile] Image loading + placeholders |
| SMKT-S037 | [Mobile] Empty list: 'No items yet' CTA |
| SMKT-S038 | [Mobile] Item count badge on category chips |
| SMKT-S039 | [Mobile] 'My Listings' section |
| SMKT-S040 | [Mobile] Final polish: card design, margins, responsive |

## CS TODOs (30)


### Threat Modeling (C001-C006)

| Code | TODO |
|------|------|
| SMKT-C001 | Architecture diagram + data flows + trust boundaries |
| SMKT-C002 | Endpoint inventory (method, inputs, auth, sensitivity) |
| SMKT-C003 | STRIDE analysis on main flow |
| SMKT-C004 | Attack surface mapping (all untrusted inputs) |
| SMKT-C005 | Attack tree (3+ paths, AND/OR nodes) |
| SMKT-C006 | 1-page threat assessment (top 5 risks) |

### Code Review / Bug Bounty (C007-C016)

| Code | TODO |
|------|------|
| SMKT-C007 | Find SQL Injection (CWE-89) |
| SMKT-C008 | Find Stored XSS (CWE-79) |
| SMKT-C009 | Find IDOR (CWE-639) |
| SMKT-C010 | Find hardcoded secrets (CWE-798) |
| SMKT-C011 | Find weak password hashing (CWE-328) |
| SMKT-C012 | Audit missing authentication (CWE-306) |
| SMKT-C013 | Analyze CORS configuration |
| SMKT-C014 | Find debug mode exposure (CWE-489) |
| SMKT-C015 | Review error handling (CWE-209) |
| SMKT-C016 | Audit SE teammates' code (2+ bug reports) |

### Security Testing (C017-C024)

| Code | TODO |
|------|------|
| SMKT-C017 | SQLi exploit script |
| SMKT-C018 | XSS exploit (3 payloads) |
| SMKT-C019 | IDOR enumeration script |
| SMKT-C020 | Rate limit / DoS test |
| SMKT-C021 | Automated scan (nmap + Python) |
| SMKT-C022 | API fuzzer |
| SMKT-C023 | Project-specific exploit (Werkzeug/pickle/CSRF/etc.) |
| SMKT-C024 | test_security.py (8+ pytest cases) |

### Deployment & Hardening (C025-C030)

| Code | TODO |
|------|------|
| SMKT-C025 | Fix SQL Injection (parameterized queries) |
| SMKT-C026 | Add authentication + fix IDOR |
| SMKT-C027 | Secure configuration (debug, secrets, passwords) |
| SMKT-C028 | Security headers + rate limiting |
| SMKT-C029 | Structured logging + alerting |
| SMKT-C030 | Final 5-page audit report |
