#!/bin/bash
# deploy-framework.sh - Pour créer le mini-framework JAR

# Configuration
PROJECT_ROOT="$PWD"
BUILD_DIR="$PROJECT_ROOT/build"
CLASSES_DIR="$BUILD_DIR/classes"
LIB_DEST="/Users/andriamalalatojo/Documents/Lib"

echo "=== Nettoyage ancien build ==="
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
fi
mkdir -p "$CLASSES_DIR"

echo "=== Compilation Java ==="
# Création du CLASSPATH
CLASSPATH=""
for jar in "$PROJECT_ROOT/lib"/*.jar; do
    if [ -f "$jar" ]; then
        if [ -z "$CLASSPATH" ]; then
            CLASSPATH="$jar"
        else
            CLASSPATH="$CLASSPATH:$jar"
        fi
    fi
done

# Compilation
find "$PROJECT_ROOT/src/main/java" -name "*.java" -exec javac -cp "$CLASSPATH" -d "$CLASSES_DIR" {} +
if [ $? -ne 0 ]; then
    echo "Erreur compilation"
    exit 1
fi

echo "=== Création du JAR ==="
if [ -f "$PROJECT_ROOT/mini-framework.jar" ]; then
    rm "$PROJECT_ROOT/mini-framework.jar"
fi
cd "$CLASSES_DIR"
jar -cvf "$PROJECT_ROOT/mini-framework.jar" * > /dev/null

echo "=== Copie vers les destinations ==="
# Copie vers /Users/andriamalalatojo/Documents/Lib
if [ ! -d "$LIB_DEST" ]; then
    mkdir -p "$LIB_DEST"
fi
cp "$PROJECT_ROOT/mini-framework.jar" "$LIB_DEST/"

# Copie vers le projet test
TEST_LIB="/Users/andriamalalatojo/Documents/ITU/S5/Mr Naina/test/lib"
if [ ! -d "$TEST_LIB" ]; then
    mkdir -p "$TEST_LIB"
fi
cp "$PROJECT_ROOT/mini-framework.jar" "$TEST_LIB/"

echo "Mini-framework JAR créé et copié dans:"
echo "  - $LIB_DEST"
echo "  - $TEST_LIB"
cd "$PROJECT_ROOT"