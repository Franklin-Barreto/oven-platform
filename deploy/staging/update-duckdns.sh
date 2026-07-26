#!/bin/sh

set -eu

: "${DUCKDNS_DOMAIN:?DUCKDNS_DOMAIN is required}"
: "${DUCKDNS_TOKEN:?DUCKDNS_TOKEN is required}"

response="$(
  printf \
    'url = "https://www.duckdns.org/update?domains=%s&token=%s&ip=&verbose=true"\n' \
    "$DUCKDNS_DOMAIN" \
    "$DUCKDNS_TOKEN" \
    | curl \
        --fail \
        --silent \
        --show-error \
        --config -
)"

case "$response" in
  OK*)
    printf '%s\n' "$response"
    ;;
  *)
    printf 'DuckDNS update failed: %s\n' "$response" >&2
    exit 1
    ;;
esac