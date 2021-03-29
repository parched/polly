Feature: creating and reading the blocks.

Background:
    * url 'http://localhost:7000'
    * configure retry = { count: 10, interval: 500 }
    * call read('common.feature')

Scenario: The initial blocks are empty
    Given path 'blocks'
    When method get
    Then status 200
    And match response == []

Scenario: A created block is added to the chain
    Given path 'data'
    And request 'abcd'
    When method post
    Then status 200
    And match response == 'creating block'

    Given path 'blocks'
    And retry until response.length == 1
    When method get
    Then status 200
    And assert isValidChain(response)
    And match dataOf(response) == ['abcd']

Scenario: A second created block is added to the chain
    Given path 'data'
    And request 'abcd'
    When method post

    Given path 'data'
    And request 'xyz'
    When method post

    Given path 'blocks'
    And retry until response.length == 2
    When method get
    Then status 200
    And assert isValidChain(response)
    And match dataOf(response) == ['abcd', 'xyz']

Scenario: A block inserted at 0 on an empty chain is added to it
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', modifier: 17420, prev_hash: ''}}
    When method put
    Then status 200
    And match response == 'block inserted'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', modifier: 17420, prev_hash: ''}]

Scenario: A block inserted with the wrong previous hash is not added to the chain
    Given path 'block'
    # TODO test: modifer is wrong
    And request {index: 0, block: {data: 'YWJjZA==', modifier: 17420, prev_hash: 'wrongBecauseItShouldBeEmptyForFirstBlock'}}
    When method put
    # TODO impl: should be an error response
    Then status 200
    And match response == 'block inserted'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == []

Scenario: A block inserted with the wrong modifier is not added to the chain
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', modifier: 1, prev_hash: ''}}
    When method put
    # TODO impl: should be an error response
    Then status 200
    And match response == 'block inserted'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == []

Scenario: A block inserted at 0 twice does only add one block
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', modifier: 17420, prev_hash: ''}}
    When method put
    Then status 200

    Given path 'block'
    # TODO: modifer is wrong
    And request {index: 0, block: {data: 'otherData999', modifier: 17420, prev_hash: ''}}
    When method put
    # TODO impl: should be an error response
    Then status 200

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', modifier: 17420, prev_hash: ''}]

Scenario: A block inserted at the end of non-empty chain is added
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', modifier: 17420, prev_hash: ''}}
    When method put
    Then status 200

    Given path 'block'
    And request {index: 1, block: {data: 'eHl6', modifier: 72769, prev_hash: 'g05oOxpRu844SXlcmMsrbWZtJ4xe5VYsHgJFr0idAAA='}}
    When method put
    Then status 200

    Given path 'blocks'
    When method get
    Then status 200
    And match response ==
    """
    [
        {data: 'YWJjZA==', modifier: 17420, prev_hash: ''},
        {data: 'eHl6', modifier: 72769, prev_hash: 'g05oOxpRu844SXlcmMsrbWZtJ4xe5VYsHgJFr0idAAA='}
    ]
    """
