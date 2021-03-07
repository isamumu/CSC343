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

-- relate scheduled flights to times for domestic 
CREATE VIEW scheduledDOM AS
SELECT domestics.flight_id as id, flight.s_dep as dep, flight.s_arv as arv
FROM flight JOIN domestics on domestics.flight_id = flight.id;

-- relate scheduled flights to times for international
CREATE VIEW scheduledINT AS
SELECT internationals.flight_id as id, flight.s_dep as dep, flight.s_arv as arv
FROM flight JOIN internationals on internationals.flight_id = flight.id;

-- relate actual flights to times for domestic 
-- NOTE: there are flights that haven't departed yet!
CREATE VIEW realDOM AS
SELECT domestics.flight_id as id, departure.datetime as dep, arrival.datetime as arv
FROM domestics JOIN departure on domestics.flight_id = departure.flight_id
               JOIN arrival on domestics.flight_id = arrival.flight_id;

-- relate actual flights to times for international
CREATE VIEW realINT AS
SELECT internationals.flight_id as id, departure.datetime as dep, arrival.datetime as arv
FROM internationals JOIN departure on internationals.flight_id = departure.flight_id
               JOIN arrival on internationals.flight_id = arrival.flight_id;

-- find a relation of % delays in domestic flights, which filters out flights that have not departed yet
CREATE VIEW delayDOM AS
SELECT domestics.flight_id as id, (realDOM.dep - scheduledDOM.dep) as depDelay, (realDOM.arv - scheduledDOM.arv) as arvDelay
FROM domestics JOIN realDOM on domestics.flight_id = realDOM.id
               JOIN scheduledDOM on domestics.flight_id = scheduledDOM.id;

CREATE VIEW delayINT AS
SELECT internationals.flight_id as id, (realINT.dep - scheduledINT.dep) as depDelay, (realINT.arv - scheduledINT.arv) as arvDelay
FROM internationals JOIN realINT on internationals.flight_id = realINT.id
               JOIN scheduledINT on internationals.flight_id = scheduledINT.id;

-- find flights that did not make up for the delay
CREATE VIEW refundDOM AS
SELECT id, depDelay
FROM delayDOM
WHERE arvDelay > depDelay / 2 and depDelay > '05:00:00';

CREATE VIEW refundINT AS
SELECT id, depDelay
FROM delayINT
WHERE arvDelay > depDelay / 2 and depDelay > '08:00:00';

-- -- find pricing of domestic flights
-- CREATE VIEW priceDOM AS
-- SELECT id, depDelay, economy, business, first
-- FROM refundDOM join price on refundDOM.id = price.flight_id;

-- -- find pricing of international flights
-- CREATE VIEW priceINT AS
-- SELECT id, depDelay, economy, business, first
-- FROM refundINT join price on refundINT.id = price.flight_id;

-- find domestic flights of 35% refund
CREATE VIEW thirtyfiveDOM AS
SELECT booking.flight_id as id, 0.35 * price as refund, seat_class
FROM refundDOM join booking on refundDOM.id = booking.flight_id
WHERE depDelay >= '00:05:00' and depDelay < '00:10:00'; 

-- find domestic flights of 50% refund
CREATE VIEW fiftyDOM AS
SELECT booking.flight_id as id, 0.5 * price as refund, seat_class
FROM refundDOM join booking on refundDOM.id = booking.flight_id
WHERE depDelay >= '00:10:00'; 

-- find international flights of 35% refund
CREATE VIEW thirtyfiveINT AS
SELECT booking.flight_id as id, 0.35 * price as refund, seat_class
FROM refundINT join booking on refundINT.id = booking.flight_id
WHERE depDelay >= '00:08:00' and depDelay < '00:12:00'; 

-- find international flights of 50% refund
CREATE VIEW fiftyINT AS
SELECT booking.flight_id as id, 0.5 * price as refund, seat_class
FROM refundINT join booking on refundINT.id = booking.flight_id
WHERE depDelay >= '00:12:00'; 

CREATE VIEW refunds AS
(SELECT * FROM thirtyfiveDOM) UNION (SELECT * FROM fiftyDOM) UNION (SELECT * FROM thirtyfiveDOM) UNION (SELECT* FROM fiftyINT);

CREATE VIEW refundLabels AS
SELECT flight.airline, airline.name, EXTRACT(year FROM departure.datetime) as year, refunds.seat_class as seat_class, sum(refunds.refund) as refund
FROM refunds JOIN flight on refunds.id = flight.id 
             JOIN airline on flight.airline = airline.code
             JOIN departure on refunds.id = departure.flight_id
GROUP BY flight.airline, airline.name, refunds.seat_class, departure.datetime;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q2
