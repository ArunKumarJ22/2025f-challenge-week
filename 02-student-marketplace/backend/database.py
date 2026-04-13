"""
Database helper for the Student Marketplace.
Uses MySQL for data storage
"""

import os
import mysql.connector

DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "localhost"),
    "port": int(os.environ.get("DB_PORT", "3306")),
    "user": os.environ.get("DB_USER", "root"),
    "password": os.environ.get("DB_PASSWORD", "Arun22"),
    "database": os.environ.get("DB_NAME", "student_marketplace"),
}

def health_check():
    """Check if the database connection is working."""
    try:
        conn = get_db()
        conn.close()
        print("✅ Database connection successful")
    except Exception as e:
        print("❌ Database connection failed:", e)

def get_db():
    """Return a connection to the MySQL database."""
    return mysql.connector.connect(**DB_CONFIG)


def init_db():
    """Create the database and items table if they do not exist."""
    config_no_db = {k: v for k, v in DB_CONFIG.items() if k != "database"}
    conn = mysql.connector.connect(**config_no_db)
    cursor = conn.cursor()
    cursor.execute(f"CREATE DATABASE IF NOT EXISTS {DB_CONFIG['database']}")
    cursor.close()
    conn.close()

    conn = get_db()
    cursor = conn.cursor()
    cursor.execute(
        """
        CREATE TABLE IF NOT EXISTS items (
            id INT AUTO_INCREMENT PRIMARY KEY,
            title VARCHAR(255) NOT NULL,
            description TEXT,
            price DOUBLE NOT NULL,
            category VARCHAR(100) NOT NULL,
            image_url TEXT,
            seller_name VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            is_sold TINYINT(1) DEFAULT 0
        )
        """
    )
    conn.commit()
    cursor.close()
    conn.close()

if __name__ == "__main__":
    init_db()
    health_check()