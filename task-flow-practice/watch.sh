#!/bin/sh
# Dev live-reload loop for the taskflow backend.
# Runs inside the Dockerfile.dev container.
# Watches *.java and pom.xml files; rebuilds and restarts the jar on any change.

APP_JAR="/app/taskflow-http/target/taskflow.jar"
APP_PID=""

build() {
  echo "[watch] Building..."
  if mvn package -DskipTests -q -f /app/pom.xml; then
    echo "[watch] Build OK"
    return 0
  else
    echo "[watch] Build FAILED — keeping previous process"
    return 1
  fi
}

start() {
  java -jar "$APP_JAR" &
  APP_PID=$!
  echo "[watch] Started (pid=$APP_PID)"
}

stop() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=""
  fi
}

# Initial build + start
build && start

# Watch loop — re-trigger on any .java or pom.xml change
while inotifywait -r -q -e modify,create,delete \
      --include='\.(java|xml)$' \
      /app; do
  echo "[watch] Change detected"
  stop
  build && start
done
