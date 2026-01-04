# Multi-stage build para otimizar o tamanho da imagem
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar arquivos de dependências primeiro (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fonte e compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Imagem final
FROM eclipse-temurin:21-jre-alpine

# Instalar wget para health check
RUN apk add --no-cache wget

WORKDIR /app

# Copiar o JAR compilado
COPY --from=build /app/target/*.jar app.jar

# Criar usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring:spring

# Expor porta
EXPOSE 8080

# Variáveis de ambiente padrão (podem ser sobrescritas)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Executar aplicação
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
