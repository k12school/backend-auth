#!/bin/sh

# Replace the spec-url in the HTML file
sed -i "s|https://cdn.redocly.com/redoc/museum-api.yaml|${SPEC_URL}|g" /usr/share/nginx/html/index.html

# Start nginx
exec nginx -g 'daemon off;'
