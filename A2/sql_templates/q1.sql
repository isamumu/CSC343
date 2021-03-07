-- Q1. Airlines

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q1 CASCADE;

CREATE TABLE q1 (
    pass_id INT,
    name VARCHAR(100),
    airlines INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;


-- Define views for your intermediate steps here:

-- TODO: check Everyone and Duplicate conditions
-- find a relation relating a passenger to their flight number
CREATE VIEW passengers AS 
SELECT booking.flight_id as flight_id, passenger.id as pass_id
FROM booking JOIN passenger on booking.pass_id = passenger.id;

-- find a relation relating a passenger to their airlines
CREATE VIEW airPassenger AS
SELECT passengers.pass_id as pass_id, flight.airline as airline 
FROM passengers JOIN flight on passengers.flight_id = flight.id;

-- find a relation relating a passenger to their firstname and lastname, with their passenger id 
CREATE VIEW travellers AS
SELECT distinct passengers.pass_id as pass_id, passenger.firstname as firstname, passenger.surname as lastname
FROM passengers JOIN passenger on passengers.pass_id = passenger.id;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q1
(SELECT travellers.pass_id as pass_id, travellers.firstname || ' ' || travellers.lastname as name, count(airPassenger.airline) as airlines
FROM travellers JOIN airPassenger on travellers.pass_id = airPassenger.pass_id
GROUP BY travellers.pass_id, travellers.firstname, travellers.lastname);
