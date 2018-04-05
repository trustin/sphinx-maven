#!/bin/bash -e
cd $(dirname "$0")
BASEDIR="$(pwd)"
WORKDIR="$BASEDIR/target/update-sphinx"
JYTHON_VERSION='2.7.1'
JYTHON_INSTALLER="$HOME/.m2/repository/org/python/jython-installer/$JYTHON_VERSION/jython-installer-$JYTHON_VERSION.jar"
JYTHON_HOME="$WORKDIR/jython"

# Download the Jython installer.
if [[ ! -a "$JYTHON_INSTALLER" ]]; then
  ./mvnw dependency:get -DgroupId=org.python -DartifactId=jython-installer -Dversion="$JYTHON_VERSION"
fi

# Download easy_install.
mkdir -p "$WORKDIR"
pushd "$WORKDIR" >/dev/null
if [[ ! -f "ez_setup.py" ]]; then
  curl -O "http://peak.telecommunity.com/dist/ez_setup.py"
fi
popd >/dev/null

# Install Jython and easy_install
rm -fr "$JYTHON_HOME"
java -jar "$JYTHON_INSTALLER" -s -d "$JYTHON_HOME" -t standard

"$JYTHON_HOME/bin/jython" "$WORKDIR/ez_setup.py"

## Core
"$JYTHON_HOME/bin/easy_install" \
  'PyYAML==3.12' \
  'Sphinx==1.6.7'

## Extensions
"$JYTHON_HOME/bin/easy_install" \
  'javasphinx==0.9.15' \
  'recommonmark==0.4.0' \
  'sphinxcontrib-httpdomain==1.6.1' \
  'sphinxcontrib-inlinesyntaxhighlight==0.2' \
  'sphinxcontrib-plantuml==0.11' \
  "$BASEDIR/src/build/sphinxcontrib-scaladomain-0.1a1-patched.tar.gz"

## Themes
"$JYTHON_HOME/bin/easy_install" \
  'guzzle_sphinx_theme==0.7.11' \
  'sphinx_bootstrap_theme==0.6.4' \
  'sphinx_rtd_theme==0.2.5b2'

# Extract .egg files if not extracted yet.
find "$JYTHON_HOME/Lib/site-packages" -type f -name '*.egg' | while read -r F; do
  unzip "$F" -d "$F.extracted"
  rm -f "$F"
  mv "$F.extracted" "$F"
done

# Assemble the distribution.
rsync -a "$JYTHON_HOME/Lib/site-packages/pkg_resources" "$WORKDIR/dist"
find "$JYTHON_HOME/Lib/site-packages" -type d -name 'EGG-INFO' | while read -r F; do
  PKGDIR="$(dirname "$F")"
  rsync -a --exclude 'EGG-INFO' "$PKGDIR"/ "$WORKDIR/dist"
done

# Apply the patches.
pushd "$WORKDIR/dist" >/dev/null
patch -p1 < "$BASEDIR/src/build/patch.diff"
rm 'sphinxcontrib/plantuml$py.class' 'sphinxcontrib/inlinesyntaxhighlight$py.class'
popd >/dev/null

# Convert CRLF to LF.
find "$WORKDIR/dist" '(' -name '*.svg' -or -name '*.css' -or -name '*.txt' -or -name '*.xsl' ')' -exec dos2unix -q -k '{}' ';'

# Compile all .py files.
echo "
import compileall;
compileall.compile_dir('$WORKDIR/dist');
" | "$JYTHON_HOME/bin/jython"

# Delete unnecessary '*.py' and '*.py[co]' files.
find "$WORKDIR/dist" '(' -name '*.py' -or -name '*.pyc' -or -name '*.pyo' ')' -delete

# Transfer the distribution.
rsync -aiP --delete "$WORKDIR/dist/" "$BASEDIR/src/main/resources/kr/motd/maven/sphinx/dist"

echo 'Execute the following command to complete the update process:'
echo
echo '  git add -A src/main/resources/kr/motd/maven/sphinx/dist'
echo
