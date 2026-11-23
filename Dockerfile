FROM eclipse-temurin:21-jdk-alpine

ENV LANG=C.UTF-8

WORKDIR /app

COPY backend/lib ./backend/lib
COPY backend/src/main/java ./backend/src/main/java
COPY frontend ./frontend

RUN mkdir -p out && \
    javac -encoding UTF-8 -cp backend/lib/postgresql-42.7.14.jar -d out $(find backend/src/main/java -name "*.java")

EXPOSE 8080

CMD ["java", "-cp", "out:backend/lib/postgresql-42.7.14.jar", "br.uepa.foxfit.controller.ServidorFoxFit"]
