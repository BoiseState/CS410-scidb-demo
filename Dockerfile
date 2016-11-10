FROM java:8-jre-alpine

EXPOSE 4567

# Install database
RUN apk add --no-cache supervisor postgresql bash

# Copy config files
RUN mkdir /etc/supervisor.d
# TODO Modify supervisord.conf to reference your program
COPY supervisord.conf /etc/supervisor.d/webapp.ini

CMD /usr/bin/supervisord -n

COPY initdb.sh /srv

# Copy setup scripts *including* the DB dump
# TODO Modify to include your dump or schema file
COPY scidb-dump.sql fulltext-init.sql /srv/

# Set up PostgreSQL database
RUN install -d -o postgres -g postgres /srv/psql
# TODO Modify to reference your dump file name
RUN /bin/su -c "/bin/sh /srv/initdb.sh /srv/scidb-dump.sql /srv/fulltext-init.sql" postgres
VOLUME /srv/psql

# Add the code
# TODO Modify to reference your code
ADD target/scidb-demo.tgz /srv