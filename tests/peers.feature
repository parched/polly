Feature: adding and notifying peers.

Background:
    * def testNodeUrl =  'http://localhost:7000'
    * def mockUrl = 'http://localhost:' + karate.start('mocks/peer.feature').port
    * url testNodeUrl
    * def someChain =
    """
    [
        {data: 'YWJjZA==', prev_hash: ''},
        {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}
    ]
    """
    * configure retry = { count: 10, interval: 500 }

Scenario: A peer is added
    Given path 'peers'
    And request 'http://example.com:99'
    When method put
    Then status 200
    And match response == 'peer inserted'

Scenario: New block is broadcast to peer
    Given path 'peers'
    And request mockUrl
    When method put
    Then status 200
    And match response == 'peer inserted'

    Given path 'data'
    And request 'abcd'
    When method post

    Given url mockUrl
    And path '__test/log/put/block'
    And retry until responseStatus == 200 && response.length > 0
    When method get
    Then status 200
    And match response == [{index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}]


Scenario: Block inserted past end of chain causes resolution
    # setup mock
    Given url mockUrl
    And path '__test/blocks'
    And request someChain
    And method put
    And status 200

    When url testNodeUrl
    And path 'peers'
    And request mockUrl
    And method put
    And status 200

    And path 'block'
    And request {index: 1, block: {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}}
    And method put
    And status 200

    Then path 'blocks'
    And retry until responseStatus == 200 && JSON.stringify(response) == JSON.stringify(someChain)
    And method get

