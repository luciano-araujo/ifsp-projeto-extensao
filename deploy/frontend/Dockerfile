# Frontend Creator Workbench (repositorio do Henrique) -> export estatico servido pelo nginx.
# Contexto de build = clone do frontend; o script de deploy copia nginx.conf e .dockerignore para dentro dele.
FROM public.ecr.aws/docker/library/node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# o lockfile do repositorio costuma estar fora de sincronia: npm ci falha, npm install resolve
RUN npm install --no-audit --no-fund
COPY . .
# URL da API relativa (vazia): o nginx encaminha /api/ ao backend. Nenhum .env.local entra na imagem (ver .dockerignore).
ENV NEXT_PUBLIC_API_BASE_URL="" NODE_ENV=production NEXT_TELEMETRY_DISABLED=1
RUN rm -f .env.local .env.production && npx next build

FROM public.ecr.aws/nginx/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/out /usr/share/nginx/html
EXPOSE 8081
