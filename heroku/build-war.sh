#!/usr/bin/env bash
set -e
cd "$(dirname "$(readlink -e "$0")")"/..

# create build dir
rm -rf out/heroku
mkdir out/heroku

# copy war and include right web.xml
cp -r out/planlist.war out/heroku
cd heroku/web
zip -rq ../../out/heroku/planlist.war .
cd ../..

# copy tmp, webapp-runner.jar and Procfile
cp -r heroku/tmp out/heroku
cp -r heroku/Procfile out/heroku
cp /work/code/java/lib/webapp-runner-7.0.57.2.jar out/heroku/webapp-runner.jar

# copy it to Dropbox
rm -r /work/Dropbox/Apps/Heroku/planlist
cp -r out/heroku /work/Dropbox/Apps/Heroku/planlist
