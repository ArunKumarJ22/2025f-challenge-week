Structured Logging Configuration (C029)
Replaces all print() statements with JSON-formatted logs.
"""

import logging
import json
from logging.handlers import SMTPHandler

# Configure JSON logging
logging.basicConfig(
    level=logging.INFO,
    format='{"time": "%(asctime)s", "level": "%(levelname)s", "message": "%(message)s"}'
)

# Create logger instance
logger = logging.getLogger(__name__)
which integrated with app.py and database.py
