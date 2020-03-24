#!/bin/sh
set -e

psql -U postgres <<DDL
CREATE DATABASE user_service;
REVOKE CONNECT ON DATABASE user_service FROM PUBLIC;
CREATE USER user_service WITH PASSWORD 'user_service';
GRANT CONNECT ON DATABASE user_service TO user_service;
DDL

psql user_service < /docker-entrypoint-initdb.d/user_service.sql.dump
