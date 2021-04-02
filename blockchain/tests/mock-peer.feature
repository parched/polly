Feature: stateful mock server

Background:
    * def logPutBlock = []
    * def blocks = []

Scenario: pathMatches('/block') && methodIs('put')
    * set logPutBlock[] = request

Scenario: pathMatches('/blocks') && methodIs('get')
    * def response = blocks

# for the test to configure and read from the mock

Scenario: pathMatches('/__test/log/put/block') && methodIs('get')
    * def response = logPutBlock

Scenario: pathMatches('/__test/blocks') && methodIs('put')
    * def blocks = request

Scenario:
    # catch-all
    * def responseStatus = 404
    * def responseHeaders = { 'Content-Type': 'text/html; charset=utf-8' }
    * def response = <html><body>Not Found</body></html>