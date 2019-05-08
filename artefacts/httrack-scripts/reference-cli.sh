CRAWL_PATH=$1
ROOT_URL=TODO
httrack "https://$ROOT_URL/cbb/schools/#site_menu_link" --depth=3 --path $CRAWL_PATH --robots=0 "-*" "+$ROOT_URL/cbb/schools/*/" "+$ROOT_URL/cbb/schools/*/20*.html" -v
