#!/bin/bash
# Usage: ./convert_to_v1.sh path/to/lineups-cli.sh

set -e

file="$1"
tmpfile="$(mktemp)"

# Read all lines up to the YEAR definition
year_line=$(grep -n '^YEAR=' "$file" | head -n1 | cut -d: -f1)
if [[ -z "$year_line" ]]; then
  echo "No YEAR definition found in $file" >&2
  exit 1
fi

echo "#!/bin/bash\n\nsource \$PBP_SRC_ROOT/artefacts/httrack-scripts/v1_import.sh\n" > "$tmpfile"

# Extract everything from the YEAR definition onward
awk 'BEGIN{p=0} /^YEAR=/ {p=1} p{print}' "$file" >> "$tmpfile"

# Extract everything up to and including the array definition
awk '
  BEGIN { in_array=0 }
  /^array=\(/ { in_array=1 }
  in_array { print; if (/^\)/) { in_array=0; exit } next }
  { print }
' "$tmpfile" > "$tmpfile.arr"

# Append the import_data_v1 call
echo "\nimport_data_v1 \"\$YEAR\" \"\$CONF\" \"\${array[@]}\"" >> "$tmpfile.arr"

# Replace
cp "$tmpfile.arr" "$file"
rm -f "$tmpfile" "$tmpfile.arr"