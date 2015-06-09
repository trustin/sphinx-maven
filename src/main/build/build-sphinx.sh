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

#pip install javasphinx -t $PWD

#pip install sphinxcontrib-plantuml -t $PWD

zip -r ../sphinx.zip *

mv ../sphinx.zip "$BASE_DIR/src/main/resources/"