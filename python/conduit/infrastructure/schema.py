"""DDL for the 7-table Conduit schema (parity with V1__create_tables.sql).

Kept as a single source of truth used by both the Alembic migration and the
test-database bootstrap helper.
"""

from __future__ import annotations

from typing import List

from sqlalchemy.engine import Connection
from sqlalchemy import text

SCHEMA_STATEMENTS: List[str] = [
    """
    create table users (
      id varchar(255) primary key,
      username varchar(255) UNIQUE,
      password varchar(255),
      email varchar(255) UNIQUE,
      bio text,
      image varchar(511)
    )
    """,
    """
    create table articles (
      id varchar(255) primary key,
      user_id varchar(255),
      slug varchar(255) UNIQUE,
      title varchar(255),
      description text,
      body text,
      created_at TIMESTAMP NOT NULL,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
    """,
    """
    create table article_favorites (
      article_id varchar(255) not null,
      user_id varchar(255) not null,
      primary key(article_id, user_id)
    )
    """,
    """
    create table follows (
      user_id varchar(255) not null,
      follow_id varchar(255) not null
    )
    """,
    """
    create table tags (
      id varchar(255) primary key,
      name varchar(255) not null
    )
    """,
    """
    create table article_tags (
      article_id varchar(255) not null,
      tag_id varchar(255) not null
    )
    """,
    """
    create table comments (
      id varchar(255) primary key,
      body text,
      article_id varchar(255),
      user_id varchar(255),
      created_at TIMESTAMP NOT NULL,
      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
    """,
]

DROP_STATEMENTS: List[str] = [
    "drop table if exists comments",
    "drop table if exists article_tags",
    "drop table if exists tags",
    "drop table if exists follows",
    "drop table if exists article_favorites",
    "drop table if exists articles",
    "drop table if exists users",
]


def create_schema(connection: Connection) -> None:
    for statement in SCHEMA_STATEMENTS:
        connection.execute(text(statement))


def drop_schema(connection: Connection) -> None:
    for statement in DROP_STATEMENTS:
        connection.execute(text(statement))
