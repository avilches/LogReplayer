LogReplayer
===========

Apache log replayer Groovy script

## Requirements

Groovy installed & http-builder 0.6:

    sudo apt-get install groovy
    grape install org.codehaus.groovy.modules.http-builder http-builder 0.6

## Usage

    groovy LogReplayer.groovy <host> <logFile>

Example:

    groovy LogReplayer.groovy http://localhost:8080 /var/log/apache2/localhost.log

## How it works:

The script reads an Apache log and execute only the GET lines. The script use the timestamps from the log,
replicating the original behavior.

The output will be something like this:

    [6] 00.147s 200  http://localhost:8080/web/book/9788490192054?val=orr
    [7] 00.058s 500! http://localhost:8080//location/free?ip=2.2.2.2 Internal Server Error

  * The [6] means now there are 6 connections opened against the server (concurrent users)
  * The second number (00.147s) means the time needed by the script to process the request
  * The third number (200, 500!) is the http code. 500! means an exception happens when processing the request
  * The fourth part is the url
  * The fifth part is the exception message, if available

## Disclaimer

This code is provided AS-IS, with no warranty (explicit or implied) and has not been vetted or tested for deployment in a production environment. Use of this code at your own risk.

Released under Public Domain. Associated libraries carry their own individual licenses where applicable.

