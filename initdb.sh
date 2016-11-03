#!/bin/sh

set -e

DBFILE="$1"
if test -z "$DBFILE"; then
    echo "no database init file" >&2
fi

# create a Postgres database
initdb --auth=trust /srv/psql

# start up the database
pg_ctl start -w -D /srv/psql

# and load data
if [ -n "$DBFILE" ]; then
    echo "Loading from $DBFILE" >&2
    psql -f "$DBFILE"
fi