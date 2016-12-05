# Dockerfile

# To use for Java/Spark programs, do the following:
# 1. Modify this file and supervisord.conf according to TODO comments
# 2. Export database to 'mydb-dump.sql' with pg_dump
# 3. Edit setup.sql to contain additional SQL code to run for your database
# 4. Run with `docker build -t myapp --build-arg dbname=mydb --build-arg appname=myapp .`
#    - mydb should be the name you use for your SQL dump (mydb-dump), and will be the
#      name of the PostgreSQL database
#    - myapp should be the name of your Maven project (artifact-id).

FROM java:8-jre-alpine

EXPOSE 4567

# Install database
RUN apk add --no-cache supervisor postgresql bash

# Copy config files
RUN mkdir /etc/supervisor.d
# TODO Modify supervisord.conf to reference your program
COPY supervisord.conf /etc/supervisor.d/webapp.ini

CMD /usr/bin/supervisord -n

COPY initdb.sh setup.sql /srv/

ARG dbname
ARG appname

# Copy setup scripts *including* the DB dump
COPY $dbname-dump.sql /srv/

# Set up PostgreSQL database
RUN install -d -o postgres -g postgres /srv/psql
RUN /bin/su -c "/bin/sh /srv/initdb.sh $dbname /srv/$dbname-dump.sql /srv/setup.sql" postgres
VOLUME /srv/psql

# Add the code
ADD target/$appname.tgz /srv