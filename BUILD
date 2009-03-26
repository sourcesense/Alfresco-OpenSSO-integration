* Before building, configure JVM heap memory:

$ export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=128m"

Ensure that OpenSSO is running. Then edit file src/test/resources/AMConfig.properties, changing:

com.iplanet.am.naming.url:  URL OpenSSO naming service
com.sun.identity.agents.app.username: OpenSSO's agent username
com.iplanet.am.service.password: OpenSSO's agent password 

* Commons operations:

Delete all generated files, and alfresco runtime stuff:

$ mvn clean

Compile:

$ mvn compile

Create jar (also run unit tests):

$ mvn package

Install to local repo (also run unit tests):

$ mvn install

Run alfresco embedded in jetty with the filter deployed:

$ mvn -Pintegration jetty:run-exploded

Run alfresco embedded in jetty with the filter deployed in debug mode (port 8000): 

$ mvnDebug -Pintegration jetty:run-exploded

Run selenium server:

$ mvn selenium:start-server

Run integration tests (external running openSSO required; will launch selenium-server and alfresco)

$ mvn -Pintegration install
