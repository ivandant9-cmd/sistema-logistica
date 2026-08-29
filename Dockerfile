# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia arquivos do Maven para baixar dependências com cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 2. Copia todo o código-fonte
COPY src ./src

# 3. Garante que a pasta frontend esperada pelo Vaadin exista na raiz do container,
# copiando os estilos de dentro de src/main/resources ou src/main/frontend se existirem,
# ou criando um vinculo para que o build encontre os arquivos.
# Vamos copiar a pasta de recursos inteira para garantir.
COPY src/main/resources ./src/main/resources

# Cria a pasta frontend na raiz exigida pelo plugin e copia os estilos se necessário
RUN mkdir -p /app/frontend && \
    if [ -d "./src/main/frontend" ]; then cp -r ./src/main/frontend/* /app/frontend/; fi

# 4. Compila a aplicação e empacota para produção
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o JAR final gerado do estágio de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
# Define limites de memória para a JVM não estourar os 512MB do Render Free
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]