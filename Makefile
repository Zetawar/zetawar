.PHONY: build-staging build-prod deploy-staging deploy-prod

build-staging:
	boot build -e staging

build-prod:
	boot build -e prod

deploy-staging: build-staging
	./bin/deploy -b staging.zetawar.com -P

deploy-prod: build-prod
	./bin/deploy -b www.zetawar.com -P
