build-staging:
	BOOT_JVM_OPTIONS= boot build -e staging

build-prod:
	BOOT_JVM_OPTIONS= boot build -e prod

deploy-staging:
	BOOT_JVM_OPTIONS= boot build -e staging
	./bin/deploy -b staging.zetawar.com -P

deploy-prod:
	BOOT_JVM_OPTIONS= boot build -e prod
	./bin/deploy -b www.zetawar.com -P
