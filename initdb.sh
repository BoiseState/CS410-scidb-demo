#!/bin/sh

set -e

if test -z "$1"; then
    echo "no database init file" >&2
fi

# create a Postgres database
initdb --auth=trust /srv/psql

# start up the database
pg_ctl start -w -D /srv/psql

# and load data
for DBFILE in "$@"; do
    echo "Loading from $DBFILE" >&2
    psql -f "$DBFILE"
done