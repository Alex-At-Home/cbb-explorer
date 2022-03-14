# Need to set up authentication first, ie "cookies.txt" in the working directory in the format
# (NOT the EFF_CRAWL_PATH, the _working_ path!)
# $EFF_ROOT_URL      TRUE    /       FALSE   1999999999      PHPSESSID     COOKIE_FRAGMENT
# Check if we have a valid cookie:
if ! curl --cookie $PWD/cookies.txt -I "https://$EFF_ROOT_URL/summary.php" | grep "HTTP/[0-9.]* 200"; then
  echo "[ERROR] You must create an authentication context first"
  exit -1
fi

# Clears this year's data from the cache before rerunning a full crawl:
if [ "$1" != "--retry" ]; then
  if [ -e $EFF_CRAWL_PATH/hts-cache/old.zip ]; then
    zip  -d  $EFF_CRAWL_PATH/hts-cache/new.zip  \
      $(unzip -l $EFF_CRAWL_PATH/hts-cache/new.zip | grep 'team=[a-zA-Z0-9]' | grep -v '&y=' | awk  '{print $4}')
  fi
  if [ -e $EFF_CRAWL_PATH/hts-cache/new.zip ]; then
    zip  -d  $EFF_CRAWL_PATH/hts-cache/old.zip  \
      $(unzip -l $EFF_CRAWL_PATH/hts-cache/old.zip | grep 'team=[a-zA-Z0-9]' | grep -v '&y=' | awk  '{print $4}')
  fi
fi

httrack "https://$EFF_ROOT_URL" --continue --depth=3 --path $EFF_CRAWL_PATH --robots=0 -N "%h%p/%n%q%[y]_%[team]_%[c]_%[s]_%[p].%t" "-*" "+$EFF_ROOT_URL/team.php*" "+$EFF_ROOT_URL/index.php?y=*" -v
