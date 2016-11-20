# ords-plugins
This project is a mavenized format for developing [Oracle ORDS plugins](http://download.oracle.com/otndocs/java/javadocs/3.0/plugin-api/getting-started.html)


## Prerequisites
* [git](http://git-scm.com/)
* [java 8](http://java.com)
* [Maven 3](http://maven.apache.org)
* [Oracle ORDS 3.0.6.176.08.46](http://www.oracle.com/technetwork/developer-tools/rest-data-services/downloads/ords-download-306-3232130.html)
* [Freemarker 2.3.25-incubating](http://www.apache.org/dyn/closer.cgi/incubator/freemarker/engine/2.3.25-incubating/binaries/apache-freemarker-2.3.25-incubating-bin.tar.gz)

## Additional steps needed
Due to licensing of Oracle software you first need to add some artifacts to 
your local maven repository before you are able to build this project.

The artifacts can be found within the downloaded ORDS zip file in `examples/plugins/lib`

Add the necessary artifacts using the following commands (Make sure mvn is in your PATH):

```shell
cd [ords-install-dir]/examples/plugins/lib

mvn install:install-file -Dfile=ojdbc6-12.1.0.2.0.jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=12.1.0.2.0 -Dpackaging=jar
mvn install:install-file -Dfile=plugin-api.jar -DgroupId=oracle.dbtools -DartifactId=plugin-api -Dversion=3.0.6.176.08.46 -Dpackaging=jar
```

## Quickstart
```shell
git clone https://github.com/gtenham/ords-plugins.git ords-plugins
cd ords-plugins
mvn clean install
```

## ORDS usage
Assuming that ORDS is installed and configured you can add the separate plugins to the ords.war
using:

``` shell
java -jar ords.war plugin ords-templating.jar
```

Do the same with the downloaded freemarker.jar (which is a dependency for the ords-templating plugin):

``` shell
java -jar ords.war plugin freemarker.jar
```

Freemarker templates are loaded from the filesystem. To configure a base path add te following
ords entry to your ORDS installation (config/ords/defaults.xml):

```xml
<entry key="templating.rootpath">/path/to/templates</entry>
```
