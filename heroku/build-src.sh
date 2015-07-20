#!/usr/bin/env bash
set -e
cd "$(dirname "$(readlink -e "$0")")"/..

# create build dir
rm -rf out/heroku
mkdir out/heroku

# copy sources, resources and configs
cp -r ../Utils/src out/heroku
cp -r src/ out/heroku
cp -r web out/heroku
cp -r heroku out
rm out/heroku/build*.sh

# copy it to Dropbox
rm -r /work/Dropbox/Apps/Heroku/planlist
cp -r out/heroku /work/Dropbox/Apps/Heroku/planlist
