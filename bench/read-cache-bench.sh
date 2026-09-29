#!/usr/bin/env bash
# Benchmarks the cached read endpoints (tags, single article, article list).
#
# Usage: bench/read-cache-bench.sh <app.jar> <label> [extra spring args...]
#   e.g. bench/read-cache-bench.sh build/libs/app.jar cache-on
#        bench/read-cache-bench.sh build/libs/app.jar cache-off --realworld.cache.enabled=false
#
# Env: PORT (8080), ARTICLES (500), DURATION (20s), CONNECTIONS (32), THREADS (4),
#      OUT_DIR (bench/results). Requires curl, jq and wrk.
set -euo pipefail

JAR=$1
LABEL=$2
shift 2
PORT=${PORT:-8080}
ARTICLES=${ARTICLES:-500}
DURATION=${DURATION:-20s}
CONNECTIONS=${CONNECTIONS:-32}
THREADS=${THREADS:-4}
OUT_DIR=${OUT_DIR:-bench/results}
BASE=http://localhost:$PORT
mkdir -p "$OUT_DIR"
DB="$OUT_DIR/$LABEL.db"
rm -f "$DB"

java -jar "$JAR" --server.port="$PORT" \
  --spring.datasource.url="jdbc:sqlite:$DB" \
  --logging.level.io.spring.infrastructure.mybatis.readservice.ArticleReadService=INFO \
  --logging.level.io.spring.infrastructure.mybatis.mapper=INFO \
  "$@" > "$OUT_DIR/$LABEL.app.log" 2>&1 &
APP_PID=$!
trap 'kill $APP_PID 2>/dev/null || true' EXIT
until curl -sf "$BASE/tags" > /dev/null; do sleep 1; done

register() {
  curl -sf -X POST "$BASE/users" -H 'Content-Type: application/json' \
    -d "{\"user\":{\"email\":\"$1@bench.io\",\"username\":\"$1\",\"password\":\"password\"}}" |
    jq -r .user.token
}
AUTHOR_TOKEN=$(register author)
READER_TOKEN=$(register reader)
curl -sf -X POST "$BASE/profiles/author/follow" -H "Authorization: Token $READER_TOKEN" > /dev/null

for i in $(seq 1 "$ARTICLES"); do
  curl -sf -X POST "$BASE/articles" -H 'Content-Type: application/json' \
    -H "Authorization: Token $AUTHOR_TOKEN" \
    -d "{\"article\":{\"title\":\"bench article $i\",\"description\":\"description $i\",\"body\":\"$(printf 'body %.0s' $(seq 1 50))\",\"tagList\":[\"tag$((i % 50))\",\"tag$(((i + 7) % 50))\",\"topic$((i % 5))\"]}}" > /dev/null
  if (( i % 3 == 0 )); then
    curl -sf -X POST "$BASE/articles/bench-article-$i/favorite" \
      -H "Authorization: Token $READER_TOKEN" > /dev/null
  fi
done

# Write-then-read consistency smoke check through the HTTP API.
SLUG=bench-article-1
curl -sf "$BASE/articles/$SLUG" > /dev/null
curl -sf -X PUT "$BASE/articles/$SLUG" -H 'Content-Type: application/json' \
  -H "Authorization: Token $AUTHOR_TOKEN" -d '{"article":{"description":"edited"}}' > /dev/null
[[ $(curl -sf "$BASE/articles/$SLUG" | jq -r .article.description) == edited ]] ||
  { echo "stale article after update" >&2; exit 1; }
curl -sf -X POST "$BASE/articles" -H 'Content-Type: application/json' \
  -H "Authorization: Token $AUTHOR_TOKEN" \
  -d '{"article":{"title":"fresh tag","description":"d","body":"b","tagList":["brand-new-tag"]}}' > /dev/null
curl -sf "$BASE/tags" | jq -e '.tags | index("brand-new-tag")' > /dev/null ||
  { echo "stale tag list after create" >&2; exit 1; }
curl -sf -X DELETE "$BASE/articles/fresh-tag" -H "Authorization: Token $AUTHOR_TOKEN" > /dev/null
[[ $(curl -s -o /dev/null -w '%{http_code}' "$BASE/articles/fresh-tag") == 404 ]] ||
  { echo "deleted article still served" >&2; exit 1; }

run() {
  local name=$1 url=$2 auth=${3:-}
  local header=()
  [[ -n $auth ]] && header=(-H "Authorization: Token $auth")
  wrk -t2 -c8 -d5s "${header[@]}" "$url" > /dev/null
  wrk -t"$THREADS" -c"$CONNECTIONS" -d"$DURATION" --latency "${header[@]}" "$url" > "$OUT_DIR/$LABEL.$name.txt"
  local rps p50 p99
  rps=$(awk '/Requests\/sec/ {print $2}' "$OUT_DIR/$LABEL.$name.txt")
  p50=$(awk '$1 == "50%" {print $2}' "$OUT_DIR/$LABEL.$name.txt")
  p99=$(awk '$1 == "99%" {print $2}' "$OUT_DIR/$LABEL.$name.txt")
  printf '%s\t%s\t%s\t%s\t%s\n' "$LABEL" "$name" "$rps" "$p50" "$p99" | tee -a "$OUT_DIR/summary.tsv"
}

run tags "$BASE/tags"
run article_anon "$BASE/articles/bench-article-250"
run article_auth "$BASE/articles/bench-article-250" "$READER_TOKEN"
run list_anon "$BASE/articles?limit=20"
run list_auth "$BASE/articles?limit=20" "$READER_TOKEN"
run list_by_tag "$BASE/articles?tag=topic3&limit=20"
