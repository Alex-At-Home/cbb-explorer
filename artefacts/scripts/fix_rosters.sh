# Just a collection of jq scripts to fix known bugs in the rosters

jq '.SeMiguel.year_class = "Sr"' $HOOPEXP_SRC_DIR/public/rosters/Men_2024/Maryland.json | sponge $HOOPEXP_SRC_DIR/public/rosters/Men_2024/Maryland.json