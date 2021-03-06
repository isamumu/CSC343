-- Q2. Refunds!

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q2 CASCADE;

CREATE TABLE q2 (
    airline CHAR(2),
    name VARCHAR(50),
    year CHAR(4),
    seat_class seat_class,
    refund REAL
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;


-- Define views for your intermediate steps here:

-- RULE: domestic flights --> 35% refund for delay >= 5hrs 
--                        --> 50% refund for delay >= 10hrs
--       international flights --> 35% refund for delay >= 8hrs
--                             --> 50% refund for delay >= 12hrs
-- BUT: if the pilot arrives at most 50% of the departure delay, refunds are dropped :( 

CREATE VIEW outCountries AS
SELECT flight.id as id, airport.country as outCountry 
FROM flight JOIN airport on flight.outbound = airport.code;

CREATE VIEW inCountries AS
SELECT flight.id as id, airport.country as inCountry 
FROM flight JOIN airport on flight.inbound = airport.code;

CREATE VIEW flightCountry AS
SELECT outCountries.id as flight_id, outCountries.outCountry as outCountry, inCountries.inCountry as inCountry
FROM outCountries JOIN inCountries on outCountries.id = inCountries.id;

-- find table of international flights
CREATE VIEW internationals AS
SELECT flight_id 
FROM flightCountry 
WHERE outCountry <> inCountry;

-- find table of domestic flights
CREATE VIEW domestics AS
SELECT flight_id 
FROM flightCountry 
WHERE outCountry = inCountry;

-- relate flights to times for domestic 

-- relate flights to times for international

-- find flights that did not make up for the delay

-- find pricing of international flights

-- find pricing of domestic flights

-- find domestic flights of 35% refund

-- find domestic flights of 50% refund

-- find international flights of 35% refund

-- find international flights of 50% refund

-- find 35% refund domestic flights that were booked relating to type of seat and find the refunded price

-- find 50% refund domestic flights that were booked relating to type of seat and find the refunded price

-- find 35% refund international flights that were booked relating to type of seat and find the refunded price

-- find 50% refund international flights that were booked relating to type of seat and find the refunded price

-- relate above 4 tables and group by code, name, year, and sum the refunds over the seat class attributes

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q2
