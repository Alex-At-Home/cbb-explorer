#REF_CRAWL_PATH=$1
#REF_ROOT_URL=$2
httrack "https://$REF_ROOT_URL/cbb/schools/#site_menu_link" --depth=3 --path $PBP_CRAWL_PATH --robots=0 "-*" "+$REF_ROOT_URL/cbb/schools/*/" "+$REF_ROOT_URL/cbb/schools/*/20*.html" -v
