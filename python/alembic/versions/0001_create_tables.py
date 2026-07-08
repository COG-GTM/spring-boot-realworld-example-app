"""create tables

Revision ID: 0001
Revises:
Create Date: 2024-01-01 00:00:00.000000

Port of V1__create_tables.sql (Flyway) — the 7-table Conduit schema.
"""
from alembic import op

from conduit.infrastructure.schema import DROP_STATEMENTS, SCHEMA_STATEMENTS

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    for statement in SCHEMA_STATEMENTS:
        op.execute(statement)


def downgrade() -> None:
    for statement in DROP_STATEMENTS:
        op.execute(statement)
