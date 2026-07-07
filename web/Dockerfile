# Stage 1: Build
FROM docker.m.daocloud.io/library/node:20-alpine AS build
WORKDIR /app
ENV NPM_CONFIG_REGISTRY=https://registry.npmmirror.com
COPY package.json package-lock.json .npmrc ./
RUN npm ci --no-audit
COPY . .
RUN npm run build

# Stage 2: Serve
FROM docker.m.daocloud.io/library/nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]