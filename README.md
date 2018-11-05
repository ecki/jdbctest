JDBC Tests
==========

In this project I collect some test code for JDBC implementations.

Building
--------

How to build (tested with Maven 3.2.3 on Java8):

    > mvn -B -e -C -V clean verify

Requires Oracle OJDB driver to compile. You can upload a version to your local maven repo cache:

    > mvn install:install-file -Dfile={Path/to/your/ojdbc8.jar} -DgroupId=com.oracle -DartifactId=ojdbc8 -Dversion=12.2.0.1 -Dpackaging=jar

You can also download a prebuild snapshot from https://github.com/ecki/jdbctest/releases


ConnectTests
------------

Used to measure connect speeds as well as instance selection.

Start with:

    > java -cp %JAR%;%OJDBC% net.eckenfels.tests.jdbc.ConnectTests [-Diterations=100] USER URL [PASS]

Currently only works with Oracle.

Sample Windows Command Line:

    > set OJDBC="%HOME%\.m2\repository\com\oracle\ojdbc8\12.2.0.1.0\ojdbc8-12.2.0.1.0.jar"
    > set JAR="target\jdbctest-0.0.2-SNAPSHOT.jar"
    > set URL="jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=10.15.79.226)(PORT=11521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1.localdomain)(SERVER=DEDICATED)))"
    > java -cp %JAR%;%OJDBC% net.eckenfels.tests.jdbc.ConnectTests SCOTT %URL% secret

sampleTest run

```text
Connection Test jre=11.0.1 Azul Systems, Inc. loc=de_DE tz=Europe/Berlin os=Windows 10/10.0

Connecting with user=SCOTT url=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=10.15.79.226)(PORT=11521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1.localdomain)(SERVER=DEDICATED)))
  iterations=100 **enc=null** oracle.jdbc.ReadTimeout=-1 oracle.net.CONNECT_TIMEOUT=-1

First Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=429,839108ms   from=(file:ojdbc8-12.2.0.1.0.jar <no signer certificates>) prod=Oracle
Second Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=87,528073ms

After 100 connects: **min=68,743963ms avg=80,680216ms max=89,144832ms**
  100 OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9

Connecting with user=SCOTT url=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=10.15.79.226)(PORT=11521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1.localdomain)(SERVER=DEDICATED)))
  iterations=100 **enc=REQUIRED** oracle.jdbc.ReadTimeout=-1 oracle.net.CONNECT_TIMEOUT=-1

First Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=553,619295ms   from=(file:ojdbc8-12.2.0.1.0.jar <no signer certificates>) prod=Oracle
Second Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=506,261773ms

After 100 connects: **min=379,090799ms avg=446,315065ms max=498,948624ms**
  100 OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9

Connecting with user=SCOTT url=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcp)(HOST=10.15.79.226)(PORT=11521))(CONNECT_DATA=(SERVICE_NAME=orclpdb1.localdomain)(SERVER=DEDICATED)))
  iterations=100 **enc=REJECTED** oracle.jdbc.ReadTimeout=-1 oracle.net.CONNECT_TIMEOUT=-1

First Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=77,261229ms   from=(file:ojdbc8-12.2.0.1.0.jar <no signer certificates>) prod=Oracle
Second Connection: OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9  time=79,286102ms

After 100 connects: **min=64,074499ms avg=79,045194ms max=89,184074ms**
  100 OK schema=SCOTT inst=1 serv=orclpdb1.localdomain on=fbd80efa8fc9```
