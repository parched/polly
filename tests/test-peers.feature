Feature: adding and notifying peers.

Background:
    * def testNodeUrl =  'http://localhost:7000'
    * def mockUrl = 'http://localhost:' + karate.start('mock-peer.feature').port
    * url testNodeUrl
    * def someChainWith2ValidBlocks =
    """
    [
        {data: 'YWJjZA==', prev_hash: ''},
        {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}
    ]
    """
    * def index0AndSomeValidBlock = {index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}
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


Scenario: Block inserted past end of chain causes resolution and broadcast
    # setup mock
    Given url mockUrl
    And path '__test/blocks'
    And request someChainWith2ValidBlocks
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
    And retry until responseStatus == 200 && JSON.stringify(response) == JSON.stringify(someChainWith2ValidBlocks)
    And method get

    And url mockUrl
    And path '__test/log/put/block'
    And retry until responseStatus == 200 && JSON.stringify(response) == JSON.stringify([{index: 1, block: someChainWith2ValidBlocks[1]}])
    And method get

Scenario: New block inserted at end of chain is broadcast to peers
    Given url testNodeUrl
    And path 'peers'
    And request mockUrl
    And method put
    And status 200

    When path 'block'
    And request index0AndSomeValidBlock
    And method put
    And status 200

    Then url mockUrl
    And path '__test/log/put/block'
    And retry until responseStatus == 200 && JSON.stringify(response) == JSON.stringify([index0AndSomeValidBlock])
    And method get

Scenario: Data removed from chain by peers is recreated on top
    # setup mock
    Given url mockUrl
    And path '__test/blocks'
    And request someChainWith2ValidBlocks
    And method put
    And status 200

    When url testNodeUrl
    And path 'peers'
    And request mockUrl
    And method put
    And status 200

    And path 'data'
    And request 'MY DATA'
    And method post
    And status 200

    And path 'block'
    And request {index: 1, block: {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}}
    And method put
    And status 200

    Then path 'blocks'
    * def isDone =
    """
    function() {
        return responseStatus == 200 &&
        response.length == 3 &&
        JSON.stringify(response.slice(0, 2)) == JSON.stringify(someChainWith2ValidBlocks)
        response.data == "TVkgREFUQQ=="
    }
    """
    And retry until isDone()
    And method get
