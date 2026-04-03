#!/bin/bash
set -e

POSTGRES="psql --username ${POSTGRES_USER}"

if [ -n "${POSTGRES_MULTIPLE_DATABASES}" ]; then
  echo "Creating multiple databases: ${POSTGRES_MULTIPLE_DATABASES}"
  IFS=',' read -ra DBS <<< "${POSTGRES_MULTIPLE_DATABASES}"
  for db in "${DBS[@]}"; do
    echo "Creating database: ${db}"
    $POSTGRES <<-EOSQL
      SELECT 'CREATE DATABASE ${db}' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${db}')\gexec
EOSQL
  done
fi
