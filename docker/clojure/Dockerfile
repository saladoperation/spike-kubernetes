FROM java:8@sha256:c1ff613e8ba25833d2e1940da0940c3824f03f802c449f3d1815a66b7f8c0e9d
COPY target/uberjar/main.jar /main.jar
EXPOSE 8080
CMD ["java", "-jar", "main.jar" "serve"]
