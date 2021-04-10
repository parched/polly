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
# waiting for it to be ready
# sending it a request (to make sure all of the http functionality is probed)
# then kill it
# TODO: replace the 'sleep 2' which waits for the broadcast with netcat -l
# TODO: put curl in the loop to avoid relying on specific output 
$(NATIVE_IMAGE_CONFIG): $(POLLY_JAR) Makefile
	rm -rf $(NATIVE_IMAGE_CONFIG_DIR)
	mkdir -p $(NATIVE_IMAGE_CONFIG_DIR)
	trap 'pollypid=`cat target/polly-pid` && kill $$pollypid && wait $$pollypid' INT TERM EXIT; \
	{ \
		"$(GRAALVM_HOME)"/bin/java \
		-agentlib:native-image-agent=config-output-dir=$(NATIVE_IMAGE_CONFIG_DIR) \
		-jar $(POLLY_JAR) & \
		echo $$! > target/polly-pid; \
	} \
	2>&1 | \
	{ \
		set -e; \
		while read line && ! { echo $$line | grep "Server online" > /dev/null; }; do echo $$line; done; \
		echo $$line; \
		curl -X PUT -d http://localhost:9999 http://localhost:8080/peers; \
		curl -X POST -d abc http://localhost:8080/data; \
		while read line && ! { echo $$line | grep "Block inserted" > /dev/null; }; do echo $$line; done; \
		echo $$line; \
		sleep 2; \
	} || { sleep 5 && rm -rf $(NATIVE_IMAGE_CONFIG_DIR) && false; }

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