#!/bin/sh
set -e

psql -U postgres <<DDL
CREATE DATABASE mailbox_service;
REVOKE CONNECT ON DATABASE mailbox_service FROM PUBLIC;
CREATE USER mailbox_service WITH PASSWORD 'mailbox_service';
GRANT CONNECT ON DATABASE mailbox_service TO mailbox_service;
DDL

psql mailbox_service < /docker-entrypoint-initdb.d/mailbox_service.sql.dump