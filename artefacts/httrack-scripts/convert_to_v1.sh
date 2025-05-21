#!/bin/bash
# Usage: ./convert_to_v1.sh path/to/lineups-cli.sh

set -e

file="$1"
tmpfile="$(mktemp)"

# Extract everything up to and including the array definition
awk '
  BEGIN { in_array=0 }
  /^array=\(/ { in_array=1 }
  in_array { print; if (/^\)/) { in_array=0; exit } next }
  { print }
' "$file" > "$tmpfile"

# Append the import_data_v1 call
echo "\nimport_data_v1 \"\$YEAR\" \"\$CONF\" \"\${array[@]}\"" >> "$tmpfile"

# Backup the original and replace
cp "$tmpfile" "$file"