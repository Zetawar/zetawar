build-staging:
	BOOT_JVM_OPTIONS= boot build -e staging

build-prod:
	BOOT_JVM_OPTIONS= boot build -e prod

deploy-staging:
	BOOT_JVM_OPTIONS= boot build -e staging
	./bin/deploy staging

deploy-prod:
	BOOT_JVM_OPTIONS= boot build -e prod
	./bin/deploy production
