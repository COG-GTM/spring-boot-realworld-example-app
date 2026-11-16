#!/usr/bin/env bash
# End-to-end smoke check of the RealWorld REST and GraphQL APIs against a running app.
# Each call is recorded as {name, status, shape} (shape = JSON types, so ids, tokens and
# timestamps are ignored) to $OUT, and semantic assertions are checked along the way.
#
# Usage: BASE_URL=http://localhost:8080 OUT=build/smoke/actual.jsonl scripts/smoke/smoke.sh
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
OUT=${OUT:-build/smoke/actual.jsonl}
RUN_ID=${RUN_ID:-$(date +%s)$RANDOM}

mkdir -p "$(dirname "$OUT")"
: >"$OUT"
BODY=$(mktemp)
HEADERS=$(mktemp)
trap 'rm -f "$BODY" "$HEADERS"' EXIT

FAILURES=0
STATUS=
LAST=

SHAPE='def shape: if type == "object" then with_entries(.value |= shape)
  elif type == "array" then (if length == 0 then [] else [.[0] | shape] end)
  else type end; shape'

# record NAME [EXTRA_JSON] — EXTRA_JSON holds stable values worth comparing (e.g. CORS headers)
record() {
  local name=$1 extra=${2:-'{}'} shape errors=null
  if [ -s "$BODY" ]; then
    shape=$(jq -cS "$SHAPE" "$BODY" 2>/dev/null || echo '"non-json"')
    errors=$(jq -c 'if type == "object" and (.errors | type) == "array"
      then [.errors[].extensions.errorType] else null end' "$BODY" 2>/dev/null || echo null)
  else
    shape=null
  fi
  jq -cnS --arg name "$name" --argjson status "$STATUS" --argjson shape "$shape" \
    --argjson errors "$errors" --argjson extra "$extra" \
    '{name: $name, status: $status, shape: $shape}
     + (if $errors == null then {} else {graphqlErrorTypes: $errors} end) + $extra' >>"$OUT"
}

# req NAME EXPECTED_STATUS METHOD PATH [BODY] [TOKEN]
req() {
  local name=$1 expected=$2 method=$3 path=$4 data=${5:-} token=${6:-}
  local args=(-s -o "$BODY" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Token $token")
  [ -n "$data" ] && args+=(--data "$data")
  STATUS=$(curl "${args[@]}" "$BASE_URL$path")
  LAST=$(cat "$BODY")
  record "$name"
  if [ "$STATUS" != "$expected" ]; then
    echo "FAIL $name: expected HTTP $expected, got $STATUS: $LAST"
    FAILURES=$((FAILURES + 1))
  else
    echo "ok   $name ($STATUS)"
  fi
}

# gql NAME QUERY [VARIABLES_JSON] [TOKEN]
gql() {
  local name=$1 query=$2 vars=${3:-'{}'} token=${4:-}
  local payload
  payload=$(jq -cn --arg q "$query" --argjson v "$vars" '{query: $q, variables: $v}')
  req "$name" 200 POST /graphql "$payload" "$token"
}

# check DESCRIPTION JQ_FILTER — asserts the filter is true for the last response body
check() {
  local desc=$1 filter=$2
  if jq -e "$filter" <<<"$LAST" >/dev/null 2>&1; then
    echo "ok     - $desc"
  else
    echo "FAIL   - $desc: $LAST"
    FAILURES=$((FAILURES + 1))
  fi
}

user_json() { jq -cn --arg u "$1" --arg e "$2" --arg p "$3" '{user: {username: $u, email: $e, password: $p}}'; }

PASSWORD=${SMOKE_PASSWORD:-smoke-$(od -An -tx1 -N12 /dev/urandom | tr -d ' \n')}
A=smokea$RUN_ID
B=smokeb$RUN_ID
C=smokec$RUN_ID

echo "== REST: users"
req rest.register 201 POST /users "$(user_json "$A" "$A@example.com" "$PASSWORD")"
check "register returns user with token" '.user.username != null and (.user.token | length > 0)'
req rest.register.b 201 POST /users "$(user_json "$B" "$B@example.com" "$PASSWORD")"
TOKEN_B=$(jq -r .user.token <<<"$LAST")
req rest.register.c 201 POST /users "$(user_json "$C" "$C@example.com" "$PASSWORD")"
TOKEN_C=$(jq -r .user.token <<<"$LAST")
req rest.register.duplicate 422 POST /users "$(user_json "$A" "$A@example.com" "$PASSWORD")"
req rest.login 200 POST /users/login "$(jq -cn --arg e "$A@example.com" --arg p "$PASSWORD" '{user: {email: $e, password: $p}}')"
TOKEN_A=$(jq -r .user.token <<<"$LAST")
check "login returns token" '.user.token | length > 0'
req rest.login.bad-password 422 POST /users/login "$(jq -cn --arg e "$A@example.com" '{user: {email: $e, password: "wrong"}}')"
req rest.current-user 200 GET /user "" "$TOKEN_A"
check "current user is A" ".user.username == \"$A\""
req rest.update-user 200 PUT /user '{"user":{"bio":"smoke bio","image":"https://example.com/a.png"}}' "$TOKEN_A"
check "bio updated" '.user.bio == "smoke bio"'

echo "== REST: profiles"
req rest.profile 200 GET "/profiles/$B" "" "$TOKEN_A"
check "not following yet" '.profile.following == false'
req rest.profile.anonymous 200 GET "/profiles/$B"
req rest.profile.missing 404 GET "/profiles/nobody$RUN_ID"
req rest.follow 200 POST "/profiles/$B/follow" "" "$TOKEN_A"
check "following after follow" '.profile.following == true'

echo "== REST: articles"
TITLE="Smoke article $RUN_ID"
req rest.article.create 200 POST /articles \
  "$(jq -cn --arg t "$TITLE" '{article: {title: $t, description: "smoke desc", body: "smoke body", tagList: ["smoke", "baseline"]}}')" "$TOKEN_B"
SLUG=$(jq -r .article.slug <<<"$LAST")
check "article created with tags" '.article.tagList | sort == ["baseline", "smoke"]'
req rest.article.get 200 GET "/articles/$SLUG"
check "article slug matches" ".article.slug == \"$SLUG\""
req rest.article.get.missing 404 GET "/articles/missing-$RUN_ID"
req rest.articles.list 200 GET "/articles?author=$B"
check "list by author has the article" '.articlesCount == 1 and .articles[0].title != null'
req rest.articles.list.by-tag 200 GET "/articles?tag=smoke&limit=1"
req rest.articles.feed 200 GET /articles/feed "" "$TOKEN_A"
check "feed contains followed author's article" "[.articles[].slug] | index(\"$SLUG\") != null"
req rest.article.update 200 PUT "/articles/$SLUG" '{"article":{"body":"updated body"}}' "$TOKEN_B"
check "article body updated" '.article.body == "updated body"'
req rest.article.update.not-owner 403 PUT "/articles/$SLUG" '{"article":{"body":"hijack"}}' "$TOKEN_A"

echo "== REST: favorites"
req rest.favorite 200 POST "/articles/$SLUG/favorite" "" "$TOKEN_A"
check "favorited" '.article.favorited == true and .article.favoritesCount == 1'
req rest.articles.list.favorited 200 GET "/articles?favorited=$A"
check "favorited list has the article" '.articlesCount == 1'
req rest.unfavorite 200 DELETE "/articles/$SLUG/favorite" "" "$TOKEN_A"
check "unfavorited" '.article.favorited == false and .article.favoritesCount == 0'

echo "== REST: comments"
req rest.comment.create 201 POST "/articles/$SLUG/comments" '{"comment":{"body":"smoke comment"}}' "$TOKEN_A"
COMMENT_ID=$(jq -r .comment.id <<<"$LAST")
check "comment body" '.comment.body == "smoke comment"'
req rest.comments.list 200 GET "/articles/$SLUG/comments"
check "one comment listed" '.comments | length == 1'
# Comments can be deleted by their author or the article's author (AuthorizationService).
req rest.comment.delete.not-owner 403 DELETE "/articles/$SLUG/comments/$COMMENT_ID" "" "$TOKEN_C"
req rest.comment.delete 204 DELETE "/articles/$SLUG/comments/$COMMENT_ID" "" "$TOKEN_A"

echo "== REST: tags"
req rest.tags 200 GET /tags
check "tags include smoke" '.tags | index("smoke") != null'

echo "== REST: unfollow and delete"
req rest.unfollow 200 DELETE "/profiles/$B/follow" "" "$TOKEN_A"
check "not following after unfollow" '.profile.following == false'
req rest.article.delete.not-owner 403 DELETE "/articles/$SLUG" "" "$TOKEN_A"
req rest.article.delete 204 DELETE "/articles/$SLUG" "" "$TOKEN_B"
req rest.article.get.deleted 404 GET "/articles/$SLUG"

echo "== Security: unauthenticated requests"
req security.current-user.no-auth 401 GET /user
req security.update-user.no-auth 401 PUT /user '{"user":{"bio":"x"}}'
req security.feed.no-auth 401 GET /articles/feed
req security.article.create.no-auth 401 POST /articles '{"article":{"title":"x","description":"x","body":"x"}}'
req security.article.update.no-auth 401 PUT "/articles/$SLUG" '{"article":{"body":"x"}}'
req security.article.delete.no-auth 401 DELETE "/articles/$SLUG"
req security.favorite.no-auth 401 POST "/articles/$SLUG/favorite"
req security.comment.create.no-auth 401 POST "/articles/$SLUG/comments" '{"comment":{"body":"x"}}'
req security.comment.delete.no-auth 401 DELETE "/articles/$SLUG/comments/1"
req security.follow.no-auth 401 POST "/profiles/$B/follow"
req security.bad-token 401 GET /user "" "not-a-jwt"
req security.articles.list.anonymous 200 GET "/articles?author=$A"
req security.tags.anonymous 200 GET /tags

echo "== Security: CORS preflight"
preflight() {
  local name=$1 path=$2 method=$3
  STATUS=$(curl -s -o /dev/null -D "$HEADERS" -w '%{http_code}' -X OPTIONS "$BASE_URL$path" \
    -H 'Origin: http://smoke.example.com' \
    -H "Access-Control-Request-Method: $method" \
    -H 'Access-Control-Request-Headers: authorization,content-type')
  tr -d '\r' <"$HEADERS" | awk -F': ' 'tolower($1) ~ /^access-control-allow-(origin|methods|headers)$/ {print tolower($1) "\t" $2}' |
    jq -R 'split("\t") | {(.[0]): .[1]}' | jq -s 'add // {}' >"$BODY"
  LAST=$(cat "$BODY")
  record "$name" "$(jq -c '{headers: .}' "$BODY")"
  if [ "$STATUS" = 200 ] && jq -e '."access-control-allow-origin" == "*"' <<<"$LAST" >/dev/null; then
    echo "ok   $name ($STATUS)"
  else
    echo "FAIL $name: HTTP $STATUS, headers $LAST"
    FAILURES=$((FAILURES + 1))
  fi
}
preflight security.cors.preflight.user /user PUT
preflight security.cors.preflight.articles /articles POST
preflight security.cors.preflight.graphql /graphql POST

echo "== GraphQL: users"
GA=gqla$RUN_ID
GB=gqlb$RUN_ID
CREATE_USER='mutation($input: CreateUserInput) { createUser(input: $input) {
  __typename
  ... on UserPayload { user { email username token profile { username bio image following } } }
  ... on Error { message errors { key value } } } }'
gql graphql.createUser "$CREATE_USER" "$(jq -cn --arg u "$GA" --arg p "$PASSWORD" '{input: {username: $u, email: ($u + "@example.com"), password: $p}}')"
check "createUser returns UserPayload" '.data.createUser.__typename == "UserPayload" and (.data.createUser.user.token | length > 0)'
gql graphql.createUser.b "$CREATE_USER" "$(jq -cn --arg u "$GB" --arg p "$PASSWORD" '{input: {username: $u, email: ($u + "@example.com"), password: $p}}')"
GTOKEN_B=$(jq -r .data.createUser.user.token <<<"$LAST")
gql graphql.createUser.duplicate "$CREATE_USER" "$(jq -cn --arg u "$GA" --arg p "$PASSWORD" '{input: {username: $u, email: ($u + "@example.com"), password: $p}}')"
check "duplicate createUser returns Error" '.data.createUser.__typename == "Error"'
gql graphql.login 'mutation($email: String!, $password: String!) { login(email: $email, password: $password) { user { email username token } } }' \
  "$(jq -cn --arg e "$GA@example.com" --arg p "$PASSWORD" '{email: $e, password: $p}')"
GTOKEN_A=$(jq -r .data.login.user.token <<<"$LAST")
check "login returns token" '.data.login.user.token | length > 0'
gql graphql.login.bad-password 'mutation($email: String!, $password: String!) { login(email: $email, password: $password) { user { token } } }' \
  "$(jq -cn --arg e "$GA@example.com" '{email: $e, password: "wrong"}')"
check "bad password returns errors" '.errors | length > 0'
gql graphql.me 'query { me { email username token profile { username bio image following } } }' '{}' "$GTOKEN_A"
check "me is A" ".data.me.username == \"$GA\""
gql graphql.updateUser 'mutation($changes: UpdateUserInput!) { updateUser(changes: $changes) { user { username profile { bio image } } } }' \
  '{"changes":{"bio":"gql bio","image":"https://example.com/g.png"}}' "$GTOKEN_A"
check "bio updated" '.data.updateUser.user.profile.bio == "gql bio"'

echo "== GraphQL: profiles"
PROFILE_FIELDS='profile { username bio image following }'
gql graphql.profile "query(\$u: String!) { profile(username: \$u) { $PROFILE_FIELDS } }" "$(jq -cn --arg u "$GB" '{u: $u}')" "$GTOKEN_A"
check "not following yet" '.data.profile.profile.following == false'
gql graphql.followUser "mutation(\$u: String!) { followUser(username: \$u) { $PROFILE_FIELDS } }" "$(jq -cn --arg u "$GB" '{u: $u}')" "$GTOKEN_A"
check "following after follow" '.data.followUser.profile.following == true'

echo "== GraphQL: articles"
ARTICLE_FIELDS='slug title description body tagList favorited favoritesCount createdAt updatedAt author { username following }'
CONN="edges { cursor node { $ARTICLE_FIELDS } } pageInfo { hasNextPage hasPreviousPage startCursor endCursor }"
gql graphql.createArticle "mutation(\$input: CreateArticleInput!) { createArticle(input: \$input) { article { $ARTICLE_FIELDS } } }" \
  "$(jq -cn --arg t "GraphQL smoke $RUN_ID" '{input: {title: $t, description: "gql desc", body: "gql body", tagList: ["smoke", "graphql"]}}')" "$GTOKEN_B"
GSLUG=$(jq -r .data.createArticle.article.slug <<<"$LAST")
check "article created" '.data.createArticle.article.slug | length > 0'
gql graphql.article "query(\$s: String!) { article(slug: \$s) { $ARTICLE_FIELDS } }" "$(jq -cn --arg s "$GSLUG" '{s: $s}')"
check "article slug matches" ".data.article.slug == \"$GSLUG\""
gql graphql.articles "query(\$a: String) { articles(first: 10, authoredBy: \$a) { $CONN } }" "$(jq -cn --arg a "$GB" '{a: $a}')"
check "articles by author has one edge" '.data.articles.edges | length == 1'
gql graphql.articles.by-tag "query { articles(first: 1, withTag: \"graphql\") { $CONN } }"
gql graphql.feed "query { feed(first: 10) { $CONN } }" '{}' "$GTOKEN_A"
check "feed contains followed author's article" "[.data.feed.edges[].node.slug] | index(\"$GSLUG\") != null"
gql graphql.updateArticle "mutation(\$s: String!) { updateArticle(slug: \$s, changes: {body: \"updated gql body\"}) { article { $ARTICLE_FIELDS } } }" \
  "$(jq -cn --arg s "$GSLUG" '{s: $s}')" "$GTOKEN_B"
check "article body updated" '.data.updateArticle.article.body == "updated gql body"'

echo "== GraphQL: favorites"
gql graphql.favoriteArticle "mutation(\$s: String!) { favoriteArticle(slug: \$s) { article { slug favorited favoritesCount } } }" "$(jq -cn --arg s "$GSLUG" '{s: $s}')" "$GTOKEN_A"
check "favorited" '.data.favoriteArticle.article.favorited == true and .data.favoriteArticle.article.favoritesCount == 1'
gql graphql.articles.favorited "query(\$f: String) { articles(first: 10, favoritedBy: \$f) { $CONN } }" "$(jq -cn --arg f "$GA" '{f: $f}')"
check "favorited list has one edge" '.data.articles.edges | length == 1'
gql graphql.unfavoriteArticle "mutation(\$s: String!) { unfavoriteArticle(slug: \$s) { article { slug favorited favoritesCount } } }" "$(jq -cn --arg s "$GSLUG" '{s: $s}')" "$GTOKEN_A"
check "unfavorited" '.data.unfavoriteArticle.article.favorited == false'

echo "== GraphQL: comments"
# Comment.article is not selected: on Boot 2.6.3 it fails with a ClassCastException in
# ArticleDatafetcher.getCommentArticle (pre-existing bug), so it is left out of the baseline.
gql graphql.addComment 'mutation($s: String!) { addComment(slug: $s, body: "gql comment") { comment { id body createdAt updatedAt author { username } } } }' \
  "$(jq -cn --arg s "$GSLUG" '{s: $s}')" "$GTOKEN_A"
GCOMMENT_ID=$(jq -r .data.addComment.comment.id <<<"$LAST")
check "comment added" '.data.addComment.comment.body == "gql comment"'
gql graphql.article.comments 'query($s: String!) { article(slug: $s) { comments(first: 10) { edges { cursor node { id body author { username } } } pageInfo { hasNextPage hasPreviousPage } } } }' \
  "$(jq -cn --arg s "$GSLUG" '{s: $s}')"
check "one comment listed" '.data.article.comments.edges | length == 1'
gql graphql.deleteComment 'mutation($s: String!, $id: ID!) { deleteComment(slug: $s, id: $id) { success } }' \
  "$(jq -cn --arg s "$GSLUG" --arg id "$GCOMMENT_ID" '{s: $s, id: $id}')" "$GTOKEN_A"
check "comment deleted" '.data.deleteComment.success == true'

echo "== GraphQL: tags, unfollow, delete"
gql graphql.tags 'query { tags }'
check "tags include graphql" '.data.tags | index("graphql") != null'
gql graphql.unfollowUser "mutation(\$u: String!) { unfollowUser(username: \$u) { $PROFILE_FIELDS } }" "$(jq -cn --arg u "$GB" '{u: $u}')" "$GTOKEN_A"
check "not following after unfollow" '.data.unfollowUser.profile.following == false'
gql graphql.deleteArticle 'mutation($s: String!) { deleteArticle(slug: $s) { success } }' "$(jq -cn --arg s "$GSLUG" '{s: $s}')" "$GTOKEN_B"
check "article deleted" '.data.deleteArticle.success == true'

echo "== GraphQL: unauthenticated"
gql graphql.me.no-auth 'query { me { username } }'
check "me without token is null or error" '.data.me == null'
gql graphql.createArticle.no-auth 'mutation { createArticle(input: {title: "x", description: "x", body: "x"}) { article { slug } } }'
check "createArticle without token returns errors" '.errors | length > 0'
gql graphql.feed.no-auth "query { feed(first: 1) { $CONN } }"

echo
if [ "$FAILURES" -gt 0 ]; then
  echo "Smoke check FAILED: $FAILURES failure(s). Results in $OUT"
  exit 1
fi
echo "Smoke check passed: $(wc -l <"$OUT") calls recorded in $OUT"
