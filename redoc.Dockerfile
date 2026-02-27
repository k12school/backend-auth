FROM redocly/redoc:latest

# Copy custom nginx configuration
COPY redoc-nginx.conf /etc/nginx/templates/default.conf.template

# The entrypoint will use envsubst on this template
