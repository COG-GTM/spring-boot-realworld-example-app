"""graphene schema equivalent to ``schema.graphqls`` (kept in sync by ``test_graphql_schema``)."""

import graphene

from conduit.graphql.mutation import Mutation
from conduit.graphql.query import Query

schema = graphene.Schema(query=Query, mutation=Mutation)
