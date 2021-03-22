Feature: creating and reading the blocks.

Background:
    * url 'http://localhost:8080'

Scenario: get initial blocks
    Given path 'blocks'
    When method get
    Then status 200
    And match response == []

Scenario: create a block
    Given path 'data'
    And request 'abcd'
    When method post
    Then status 200
    And match response == 'block created'

    Given path 'blocks'
    When method get
    Then status 200
    And match response == [{data: 'YWJjZA==', prev_hash: ''}]

Scenario: create a second block
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