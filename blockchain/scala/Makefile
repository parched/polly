all:

FORCE:

SCALA_VERSION = 3.0.0-RC2
POLLY_VERSION = 0.1.0

POLLY_JAR = target/scala-$(SCALA_VERSION)/polly-assembly-$(POLLY_VERSION).jar

$(POLLY_JAR): FORCE
	sbt assembly

POLLY_NATIVE = target/polly

NATIVE_IMAGE_CONFIG_DIR = target/native-image-config

NATIVE_IMAGE_CONFIG = $(NATIVE_IMAGE_CONFIG_DIR)/reflect-config.json

# Log what is accessed by reflection by
# running the jar with native-image-agent
# we need it to handle an incoming TCP request and an outgoing TCP request
# so we send it some data to mine and wait for it to broadcast the new block
$(NATIVE_IMAGE_CONFIG): $(POLLY_JAR) Makefile
	rm -rf $(NATIVE_IMAGE_CONFIG_DIR)
	mkdir -p $(NATIVE_IMAGE_CONFIG_DIR)
	"$(GRAALVM_HOME)"/bin/java \
	-agentlib:native-image-agent=config-output-dir=$(NATIVE_IMAGE_CONFIG_DIR) \
	-jar $(POLLY_JAR) 8080 & \
	polly_pid=$$!; \
	trap 'kill $$polly_pid && wait $$polly_pid' INT TERM EXIT; \
	set -e; \
	while ! curl -X PUT -d http://localhost:9999 http://localhost:8080/peers; \
	do sleep 1; done; \
	curl -X POST -d abc http://localhost:8080/data; \
	printf 'HTTP/1.1 200 OK\n\n[]' | netcat -l 9999

$(POLLY_NATIVE): $(POLLY_JAR) $(NATIVE_IMAGE_CONFIG)
	native-image \
	--no-fallback \
	--enable-http \
	-H:ConfigurationFileDirectories=$(NATIVE_IMAGE_CONFIG_DIR) \
	-jar $(POLLY_JAR) \
	$@

.PHONY: all
all: $(POLLY_NATIVE)

.PHONY: clean-native-image-config
clean-native-image-config:
	rm -rf $(NATIVE_IMAGE_CONFIG_DIR)