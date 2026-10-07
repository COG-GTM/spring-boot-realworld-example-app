from pathlib import Path

from django.core.management import call_command
from graphql import (
    GraphQLEnumType,
    GraphQLInputObjectType,
    GraphQLObjectType,
    GraphQLScalarType,
    GraphQLSchema,
    GraphQLUnionType,
    build_schema,
)

from conduit.graphql.schema import schema

SCHEMA_FILE = Path(__file__).resolve().parents[1] / "graphql" / "schema.graphqls"


def describe(s: GraphQLSchema) -> dict:
    """Comparable shape of a schema: kinds, fields, argument and field types, union members."""
    shape = {
        "query": s.query_type.name,
        "mutation": s.mutation_type.name if s.mutation_type else None,
    }
    for name, t in s.type_map.items():
        if name.startswith("__") or isinstance(t, GraphQLScalarType | GraphQLEnumType):
            continue
        if isinstance(t, GraphQLUnionType):
            shape[name] = ("union", sorted(m.name for m in t.types))
        elif isinstance(t, GraphQLInputObjectType):
            shape[name] = ("input", {f: str(v.type) for f, v in t.fields.items()})
        elif isinstance(t, GraphQLObjectType):
            shape[name] = (
                "type",
                {
                    f: (str(v.type), {a: str(arg.type) for a, arg in v.args.items()})
                    for f, v in t.fields.items()
                },
            )
        else:
            raise AssertionError(f"unexpected type {name}")
    return shape


def test_graphene_schema_matches_schema_graphqls():
    expected = describe(build_schema(SCHEMA_FILE.read_text()))
    actual = describe(schema.graphql_schema)
    assert actual.keys() == expected.keys()
    for name in expected:
        assert actual[name] == expected[name], name


def test_exported_sdl_stays_in_sync_with_schema_graphqls(tmp_path):
    out = tmp_path / "schema.graphql"
    call_command("graphql_schema", schema="conduit.graphql.schema.schema", out=str(out))
    assert describe(build_schema(out.read_text())) == describe(
        build_schema(SCHEMA_FILE.read_text())
    )


def test_query_and_mutation_field_names():
    s = schema.graphql_schema
    assert list(s.query_type.fields) == ["article", "articles", "me", "feed", "profile", "tags"]
    assert list(s.mutation_type.fields) == [
        "createUser",
        "login",
        "updateUser",
        "followUser",
        "unfollowUser",
        "createArticle",
        "updateArticle",
        "favoriteArticle",
        "unfavoriteArticle",
        "deleteArticle",
        "addComment",
        "deleteComment",
    ]


def test_graphql_endpoint_is_public_and_csrf_exempt(api_client, db):
    response = api_client.post("/graphql", {"query": "{ tags }"}, format="json")
    assert response.status_code == 200
    assert response.json() == {"data": {"tags": []}}
