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

pip install sphinx -t $PWD --upgrade

pip install rst2pdf -t $PWD --upgrade

pip install javasphinx -t $PWD --upgrade

pip install html5lib -t $PWD --upgrade

rm -Rf lxml*

curl -O https://pypi.python.org/packages/source/s/sphinxcontrib-plantuml/sphinxcontrib-plantuml-0.5.tar.gz
tar -xvf sphinxcontrib-plantuml-0.5.tar.gz --strip 1
rm sphinxcontrib-plantuml-0.5.tar.gz

# This does not work for sphinxcontrib-plantuml, because __init__.py is not available when installed using pip.
#pip install sphinxcontrib-plantuml --download $PWD --upgrade

find . -name "*.pyc" -delete
find . -name "*.egg-info" | xargs rm -rf
find . -name "*.dist-info" | xargs rm -rf

# Patch osutil.py file to make it work with jython 2.7.0 version.
patch -d sphinx/util -p0 < "$BASE_DIR/src/main/build/sphinx-osutil.patch"

# Patch htmlrst.py file to use html5lib html parser library instead of lxml
patch -d javasphinx -p0 < "$BASE_DIR/src/main/build/javasphinx.patch"

zip -9mrv ../sphinx.zip .
mv ../sphinx.zip "$BASE_DIR/src/main/resources/"

# Now download plantuml v8024.
#curl -O http://downloads.sourceforge.net/project/plantuml/plantuml.8024.jar
#mv plantuml.8024.jar "$BASE_DIR/src/main/resources/"

