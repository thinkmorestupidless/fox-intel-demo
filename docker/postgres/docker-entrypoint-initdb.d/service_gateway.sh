#!/bin/sh
set -e

psql -U postgres <<DDL
CREATE DATABASE service_gateway;
REVOKE CONNECT ON DATABASE service_gateway FROM PUBLIC;
CREATE USER service_gateway WITH PASSWORD 'service_gateway';
GRANT CONNECT ON DATABASE service_gateway TO service_gateway;
DDL

psql service_gateway < /docker-entrypoint-initdb.d/service_gateway.sql.dump