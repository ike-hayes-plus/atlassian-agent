#!/bin/sh
# 在共用 Postgres 上创建 Confluence 专用库/用户（与 Jira 的 jiradb/jira 分离）。
# compose 内由 conf-db-init 调用；宿主机可执行（默认 docker exec 进 jira-db 容器）。
set -eu

CONF_DB="${CONF_DB:-confdb}"
CONF_USER="${CONF_USER:-confluence}"
CONF_PASSWORD="${CONF_PASSWORD:-change-me-confluence-db-password}"
PGUSER="${PGUSER:-jira}"

psql_admin() {
  if [ -n "${PGHOST:-}" ]; then
    export PGPASSWORD="${PGPASSWORD:?PGPASSWORD required when PGHOST is set}"
    psql -h "$PGHOST" -p "${PGPORT:-5432}" -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 "$@"
  else
    PG_CONTAINER="${PG_CONTAINER:-jira-db}"
    docker exec -i "$PG_CONTAINER" psql -U "$PGUSER" -d postgres -v ON_ERROR_STOP=1 "$@"
  fi
}

db_exists="$(psql_admin -tAc "SELECT 1 FROM pg_database WHERE datname='${CONF_DB}'" 2>/dev/null || true)"
if [ "$db_exists" = "1" ]; then
  echo "Database ${CONF_DB} already exists, skip."
  exit 0
fi

user_exists="$(psql_admin -tAc "SELECT 1 FROM pg_roles WHERE rolname='${CONF_USER}'" 2>/dev/null || true)"
if [ "$user_exists" != "1" ]; then
  psql_admin -c "CREATE USER \"${CONF_USER}\" WITH PASSWORD '${CONF_PASSWORD}';"
  echo "Created role ${CONF_USER}."
else
  echo "Role ${CONF_USER} already exists, skip CREATE USER."
fi

psql_admin -c "CREATE DATABASE \"${CONF_DB}\" OWNER \"${CONF_USER}\" ENCODING 'UTF8';"
echo "Created database ${CONF_DB} (owner ${CONF_USER})."
