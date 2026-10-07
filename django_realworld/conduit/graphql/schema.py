"""GraphQL schema (port of schema.graphqls). TODO: implement; placeholder keeps /graphql up."""

import graphene


class Query(graphene.ObjectType):
    tags = graphene.List(graphene.String)

    def resolve_tags(root, info):
        return []


schema = graphene.Schema(query=Query)
