Feature: stateful mock server

Background:
    * def blocks = []

Scenario: pathMatches('/block') && methodIs('put')
    * set blocks[] = request

Scenario: pathMatches('log/put/block') && methodIs('get')
    # for the test
    * def response = blocks

Scenario:
    # catch-all
    * def responseStatus = 404
    * def responseHeaders = { 'Content-Type': 'text/html; charset=utf-8' }
    * def response = <html><body>Not Found</body></html>