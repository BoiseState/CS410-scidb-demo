#!/bin/sh

set -e

DBNAME="$1"; shift
if test -z "$DBNAME"; then
    echo "no database name" >&2
    exit 2
fi

# create a Postgres database
initdb --auth=trust /srv/psql || exit 2

# start up the database
pg_ctl start -w -D /srv/psql || exit 2

shutdown()
{
    pg_ctl stop -w -D /srv/psql
}
trap shutdown 0 INT TERM

createdb "$DBNAME" || exit 2

# and load data
for DBFILE in "$@"; do
    echo "Loading from $DBFILE" >&2
    psql -f "$DBFILE" "$DBNAME" || exit 2
done