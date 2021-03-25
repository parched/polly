Feature: creating and reading the blocks.

Background:
    * url 'http://localhost:7000'

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
    And match response == 'block created'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', prev_hash: ''}]

Scenario: A second created block is added to the chain
    Given path 'data'
    And request 'abcd'
    When method post

    Given path 'data'
    And request 'xyz'
    When method post

    Given path 'blocks'
    When method get
    Then status 200
    And match response ==
    """
    [
        {data: 'YWJjZA==', prev_hash: ''},
        {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}
    ]
    """

Scenario: A block inserted at 0 on an empty chain is added to it
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}
    When method put
    Then status 200
    And match response == 'block inserted'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', prev_hash: ''}]

Scenario: A block inserted with the wrong previous hash is not added to the chain
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', prev_hash: 'wrongBecauseItShouldBeEmptyForFirstBlock'}}
    When method put
    Then status 200
    And match response == 'block inserted'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == []

Scenario: A block inserted at 0 twice does only add one block
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}
    When method put
    Then status 200

    Given path 'block'
    And request {index: 0, block: {data: 'otherData999', prev_hash: ''}}
    When method put
    Then status 200

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', prev_hash: ''}]

Scenario: A block inserted at the end of non-empty chain is added
    Given path 'block'
    And request {index: 0, block: {data: 'YWJjZA==', prev_hash: ''}}
    When method put
    Then status 200

    Given path 'block'
    And request {index: 1, block: {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}}
    When method put
    Then status 200

    Given path 'blocks'
    When method get
    Then status 200
    And match response ==
    """
    [
        {data: 'YWJjZA==', prev_hash: ''},
        {data: 'eHl6', prev_hash: 'iNQmb9TmM40TuEX88olXnSCciXgjuSF9o+Fhk28DFYk='}
    ]
    """
