# Need to set up authentication first, ie "cookies.txt" in the working directory in the format
# (NOT the EFF_CRAWL_PATH, the _working_ path!)
# $EFF_ROOT_URL      TRUE    /       FALSE   1999999999      PHPSESSID     ADD_SESSION_ID
#EFF_CRAWL_PATH=$1
#EFF_ROOT_URL=$2

# Check if we have a valid cookie:
if ! curl -H "Cookie: PHPSESSID=$(more $PWD/cookies.txt | tail -n 1 | awk '{print $7}')" -I "https://$EFF_ROOT_URL/summary.php" | grep "HTTP/2 200"; then
  echo "Create authentication context first"
  exit -1
fi

# Clears this year's data from the cache before rerunning a full crawl:
if [ -e $EFF_CRAWL_PATH/hts-cache/old.zip ]; then
  zip  -d  $EFF_CRAWL_PATH/hts-cache/new.zip  \
    $(unzip -l $EFF_CRAWL_PATH/hts-cache/new.zip | grep 'team=[a-zA-Z0-9]' | grep -v '&y=' | awk  '{print $4}')
fi
if [ -e $EFF_CRAWL_PATH/hts-cache/new.zip ]; then
  zip  -d  $EFF_CRAWL_PATH/hts-cache/old.zip  \
    $(unzip -l $EFF_CRAWL_PATH/hts-cache/old.zip | grep 'team=[a-zA-Z0-9]' | grep -v '&y=' | awk  '{print $4}')
fi

httrack "https://$EFF_ROOT_URL" --continue --depth=3 --path $EFF_CRAWL_PATH --robots=0 -N "%h%p/%n%q%[y]_%[team]_%[c]_%[s]_%[p].%t" "-*" "+$EFF_ROOT_URL/team.php*" "+$EFF_ROOT_URL/index.php?y=*" -v
