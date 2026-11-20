#!/bin/bash
set -e

echo "================================================================="
echo ">>> [SneakerHub] Initializing Oracle Database schema & seed data"
echo "================================================================="

sqlplus -s / as sysdba @/opt/sneaker-db-scripts/00_create_schema.sql

cd /opt/sneaker-db-scripts
sqlplus -s SNEAKER_STORE/SneakerStore123@//localhost:1521/FREEPDB1 @init_all.sql

echo "================================================================="
echo ">>> [SneakerHub] Oracle Database initialized successfully!"
echo "================================================================="
