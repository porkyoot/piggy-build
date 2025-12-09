#!/bin/bash
# Script to fix project dependency references in composite builds

set -e

PARENT_DIR="/home/etoile/Projects/Minecraft/piggy-mods"

echo "=== Fixing Composite Build Dependencies ==="
echo ""

# Function to fix build.gradle dependency
fix_build_gradle() {
    local project="$1"
    local build_file="$PARENT_DIR/$project/build.gradle"
    
    if [ ! -f "$build_file" ]; then
        echo "❌ File not found: $build_file"
        return 1
    fi
    
    echo "Fixing $project/build.gradle..."
    
    # Replace the project dependency with a simple dependency on the artifact
    # In composite builds, Gradle automatically resolves included builds by their artifact coordinates
    sed -i "s|modImplementation project(path: ':piggy-lib', configuration: 'namedElements')|modImplementation 'is.pig.minecraft.lib:piggy-lib'|g" "$build_file"
    
    echo "✅ Fixed $project/build.gradle"
}

# Fix piggy-admin
fix_build_gradle "piggy-admin"

# Check if piggy-inventory also has the same issue
if grep -q "project(path: ':piggy-lib'" "$PARENT_DIR/piggy-inventory/build.gradle" 2>/dev/null; then
    fix_build_gradle "piggy-inventory"
fi

echo ""
echo "=== Verification ==="
echo "Checking fixed dependencies..."
echo ""

for project in piggy-admin piggy-inventory; do
    build_file="$PARENT_DIR/$project/build.gradle"
    if [ -f "$build_file" ]; then
        echo "--- $project/build.gradle (piggy-lib dependency) ---"
        grep -A 1 -B 1 "piggy-lib" "$build_file" || echo "No piggy-lib dependency found"
        echo ""
    fi
done

echo "=== Testing Build ==="
cd "$PARENT_DIR/piggy-build"
./gradlew build --dry-run

echo ""
echo "✅ Fix applied successfully!"
echo "Now run: ./gradlew clean build"
