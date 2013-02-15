LogReplayer
===========

Apache log replayer Groovy script

Requirements:

    Groovy installed: sudo apt-get install groovy

Usage:

    groovy LogReplayer.groovy <host> <logFile>

Example:

    groovy LogReplayer.groovy http://localhost:8080 /var/log/apache2/localhost.log

How it works:

    The script read an Apache log and execute only the GET lines. The script use the timestamps from the log,
    replicating the original behavior.

    The output will be something like this

[6] 00.147s 200  http://localhost:8080/web/book/9788490192054?val=orr
[7] 00.058s 500! http://localhost:8080//location/free?ip=2.2.2.2 Internal Server Error

The [6] means now there are 6 connections open to your servers (concurrent user)
The second number (like 00.147s) means the time, in seconds, to full process the request
The third number is the http code. 500! means an exception when processing the request
The forth part is the url
The fifth part is the exception message, if available

Troubleshooting:

    If you are having problems, try calling grape from the command line like so:

        grape resolve org.codehaus.groovy.modules.http-builder http-builder 0.6
        grape install org.codehaus.groovy.modules.http-builder http-builder 0.6

    The above commands will tell you which repository URLs grape is trying to download from if it cannot find the artifact.
    If you still having problems, you might also need to create the following file in ~/.groovy/grapeConfig.xml:

    <?xml version="1.0" encoding="utf-8"?>
    <ivysettings>
      <settings defaultResolver="downloadGrapes" />
      <resolvers>
        <chain name="downloadGrapes">
          <filesystem name="cachedGrapes">
            <ivy pattern="${user.home}/.groovy/grapes/[organisation]/[module]/ivy-[revision].xml" />
            <artifact pattern="${user.home}/.groovy/grapes/[organisation]/[module]/[type]s/[artifact]-[revision].[ext]" />
          </filesystem>
          <ibiblio name="codehaus" root="http://repository.codehaus.org/" m2compatible="true" />
          <ibiblio name="codehaus.snapshots" root="http://snapshots.repository.codehaus.org/" m2compatible="true" />
          <ibiblio name="ibiblio" m2compatible="true" />
          <ibiblio name="java.net2" root="http://download.java.net/maven/2/" m2compatible="true" />
        </chain>
      </resolvers>
    </ivysettings>
