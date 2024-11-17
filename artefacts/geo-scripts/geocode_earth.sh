#
# Not an operational script, just some snippets
#
exit -1

# Given a CSV with fields like:
#  "Abeokuta, Nigeria" or
# "Aberdeen, SD"

# Split the CSV into two files, one for US and one for non-US:

grep -E '[A-Z]{2}"$' sorted_geos.txt > sorted_geos_usa.tx
grep -v -E '[A-Z]{2}"$' sorted_geos.txt > sorted_geos_for.tx

# For the non-US files (split as needed for batching, but will normally be small enough to do in one go)
# Add/ensure there's a single column with "Location" at the top of the file

ge batch csv -p 'text' -t '${row.Location}' sorted_geos_for.txt > sorted_geos_for_enriched.csv

# For US files (split as needed for batching)
# Add/ensure there's a single column with "Location" at the top of the file

ge batch csv -p 'text' -t '${row.Location}, USA' sorted_geo_usa_N.csv > sorted_geos_usa_enriched_N.csv