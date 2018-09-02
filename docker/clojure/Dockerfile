FROM clojure:lein-2.7.1@sha256:2c3fa51b875611e90f68490bc1ea7647edb05c9618420c920ba498f3ed174add
COPY target/uberjar/main.jar /main.jar
EXPOSE 80
CMD ["java", "-jar", "main.jar" "serve"]
