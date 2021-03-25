Feature: adding and notifying peers.

Background:
    * url 'http://localhost:7000'
    * def mockPort = karate.start('mocks/peer.feature').port

Scenario: A peer is added
    Given path 'peers'
    And request 'http://example.com:99'
    When method put
    Then status 200
    And match response == 'peer inserted'

Scenario: New block is broadcast to peer
    Given path 'peers'
    And request 'http://localhost:' + mockPort
    When method put
    Then status 200
    And match response == 'peer inserted'

    Given path 'data'
    And request 'abcd'
    When method post

    * configure retry = { count: 10, interval: 500 }
    Given url 'http://localhost:' + mockPort
    And path 'log/put/block'
    And retry until responseStatus == 200 && response.length > 0
    When method get
    Then status 200
    And match response == [{index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}]

