APP = dpe-spark
include $(MAKEPATH)/Makefile

BUILDER_JAVA = $(DOCKER_REGISTRY_HOST_PORT)/$(DOCKER_USER)/java-builder:8

compile:
	docker run --rm \
		-v $(ROOT):$(ROOT) \
		-w $(BASE_COMPILE_DIR) \
		$(BUILDER_JAVA) \
		sbt assembly

clean:
	docker run --rm \
		-v $(ROOT):$(ROOT) \
		-w $(BASE_COMPILE_DIR) \
		$(BUILDER_JAVA) \
		rm -Rf $(BASE_COMPILE_DIR)/target $(BASE_COMPILE_DIR)/project/project $(BASE_COMPILE_DIR)/project/target
