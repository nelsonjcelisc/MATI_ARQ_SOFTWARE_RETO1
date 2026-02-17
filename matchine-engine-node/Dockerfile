# ---------- STAGE 1: Build ----------
    FROM node:20-alpine AS builder

    WORKDIR /app
    
    # Copiamos package.json primero (mejor cache)
    COPY package*.json ./
    
    RUN npm install
    
    # Copiamos el resto del código
    COPY . .
    
    # Compilamos TypeScript
    RUN npm run build
    
    
    # ---------- STAGE 2: Runtime ----------
    FROM node:20-alpine
    
    WORKDIR /app
    
    # Copiamos solo lo necesario
    COPY package*.json ./
    RUN npm install --omit=dev
    
    # Copiamos build compilado
    COPY --from=builder /app/dist ./dist
    COPY --from=builder /app/proto ./proto
    
    # Exponemos puerto gRPC
    EXPOSE 50051
    
    CMD ["node", "dist/server.js"]
    