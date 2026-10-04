FROM eclipse-temurin:17-jdk

WORKDIR /app
COPY . .

RUN javac LoginServer.java

EXPOSE 8080

CMD ["java", "LoginServer"]
