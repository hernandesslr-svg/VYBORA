FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY src ./src
COPY scripts ./scripts
COPY certs ./certs
COPY db ./db
COPY public ./public
ENV NODE_ENV=production
EXPOSE 8080
USER node
CMD ["npm", "start"]
