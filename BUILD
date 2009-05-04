* Before building, configure JVM heap memory:

$ export MAVEN_OPTS="-Xms256m -Xmx512m -XX:PermSize=128m"

Ensure that OpenSSO is running. Then edit file web-client/src/test/resources/AMConfig.properties, changing:

com.iplanet.am.naming.url:  URL OpenSSO naming service
com.sun.identity.agents.app.username: OpenSSO's agent username
com.iplanet.am.service.password: OpenSSO's agent password 

copy this file to ./share/src/test/resources/AMConfig.properties


* Modules

webclient: Contains integration between opensso and webclient, incuding servlet filter, authentication webscript, openSSO client class
share: Contains servlet filter for integration between share and openSSO

* Webclient

Delete all generated files, and alfresco runtime stuff:
$ mvn clean

Compile:
$ mvn compile

Create jar (also run unit tests):
$ mvn package

Install to local repo (also run unit tests):
$ mvn install

Run webclient with the filter enabled on http://localhost:8080/alfresco. The logfile is ./alfresco-sso.log 
$ mvn -Pintegration jetty:run-exploded

Debug webclient embedded in jetty in debug mode (port 8000): 
$ mvnDebug -Pintegration jetty:run-exploded

Run selenium server (usefull for debugging integration test from eclipse) 
$ mvn -Pintegration selenium:start-server

Run integration tests ( *external running openSSO required*; will launch selenium-server and alfresco)
$ mvn -Pintegration install


* Share

Delete all generated files, and share runtime stuff:
$ mvn clean

Compile:
$ mvn compile

Create jar (also run unit tests):
$ mvn package

Install to local repo: 
$ mvn install

Run share with the filter enabled on http://localhost:8888/share. To test the full integration, external OpenSSO running requires, and alfresco webclient launched by maven (see above)
$ mvn -Pintegration jetty:run-exploded

Debug share embedded in jetty in debug mode (port 8000):         
$ mvnDebug -Pintegration jetty:run-exploded
