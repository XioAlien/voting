FROM mcr.microsoft.com/devcontainers/javascript-node:1-20-bookworm AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM mcr.microsoft.com/cbl-mariner/base/nginx:1

COPY docker/nginx.conf /etc/nginx/nginx.conf.default
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=6 CMD curl -fsS http://127.0.0.1/ >/dev/null || exit 1
