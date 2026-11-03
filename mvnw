#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Maven Wrapper Startup Shell Script for POSIX / Linux / macOS / Git Bash
# ----------------------------------------------------------------------------

set -e

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

MAVEN_PROJECTBASEDIR=`dirname "$PRG"`
WRAPPER_PROP="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -n "$JAVA_HOME" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
if [ -f "$WRAPPER_PROP" ]; then
  READ_URL=`grep -E "^distributionUrl=" "$WRAPPER_PROP" | cut -d'=' -f2 | tr -d '\r'`
  if [ -n "$READ_URL" ]; then
    DIST_URL="$READ_URL"
  fi
fi

ZIP_NAME=`basename "$DIST_URL"`
DIR_NAME="${ZIP_NAME%-bin.zip}"
WRAPPER_DIR="$HOME/.m2/wrapper/dists/$DIR_NAME"
MVN_CMD="$WRAPPER_DIR/$DIR_NAME/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
  for cand in "$WRAPPER_DIR"/*/bin/mvn; do
    if [ -x "$cand" ]; then
      MVN_CMD="$cand"
      break
    fi
  done
fi

if [ ! -x "$MVN_CMD" ]; then
  echo "[Maven Wrapper] Downloading Apache Maven from $DIST_URL..."
  mkdir -p "$WRAPPER_DIR"
  ZIP_TARGET="$WRAPPER_DIR/$ZIP_NAME"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP_TARGET"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ZIP_TARGET"
  else
    echo "Error: curl or wget is required to download Maven wrapper distribution."
    exit 1
  fi
  unzip -q -o "$ZIP_TARGET" -d "$WRAPPER_DIR"
  rm -f "$ZIP_TARGET"
  for cand in "$WRAPPER_DIR"/*/bin/mvn; do
    if [ -x "$cand" ]; then
      MVN_CMD="$cand"
      break
    fi
  done
fi

if [ ! -x "$MVN_CMD" ]; then
  echo "Error: Could not locate mvn binary after extraction in $WRAPPER_DIR"
  exit 1
fi

exec "$MVN_CMD" "$@"
