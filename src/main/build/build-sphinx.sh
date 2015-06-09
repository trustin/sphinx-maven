#!/bin/bash
SCRIPT_DIR=$(cd $(dirname ${BASH_SOURCE[0]:-$0}); pwd)
BASE_DIR=$(cd "$SCRIPT_DIR/../../.."; pwd)
WORK_DIR="target/sphinx-tmp"
SITE_PACKAGES="site-packages"

pushd "$BASE_DIR" > /dev/null
rm -rf $WORK_DIR
mkdir -p $WORK_DIR
cd $WORK_DIR

LC_ALL=en_US.UTF-8
LANGUAGE=en_US

mkdir -p $SITE_PACKAGES
cd $SITE_PACKAGES

pip install sphinx -t $PWD

pip install rst2pdf -t $PWD

pip install javasphinx -t $PWD

pip install sphinxcontrib-plantuml -t $PWD

find . -name "*.pyc" -delete
find . -name "*.egg-info" | xargs rm -rf
find . -name "*.dist-info" | xargs rm -rf

# Patch osutil.py file to make it work with jython 2.7.0 version.
patch -d sphinx/util -p0 < "$BASE_DIR/src/main/build/sphinx-osutil.patch"

zip -9mrv ../sphinx.zip .
mv ../sphinx.zip "$BASE_DIR/src/main/resources/"

#cd ..
#cp -R $SITE_PACKAGES "$BASE_DIR/src/main/resources/Lib/"
