FROM nginx:alpine

# Copy custom nginx configuration with CORS proxy
COPY redoc-nginx.conf /etc/nginx/conf.d/default.conf

# Copy the custom Redoc HTML (v2 with JWT auth section)
COPY api-docs-custom.html /usr/share/nginx/html/index.html

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
