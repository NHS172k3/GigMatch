# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm install --no-fund --no-audit

COPY . .
RUN npm run build -- --configuration production

# ---- Serve stage ----
FROM nginx:alpine
COPY --from=build /app/dist/gigmatch/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
