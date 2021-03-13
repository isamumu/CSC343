-- Q5. Flight Hopping

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q5 CASCADE;

CREATE TABLE q5 (
	destination CHAR(3),
	num_flights INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS day CASCADE;
DROP VIEW IF EXISTS n CASCADE;
DROP VIEW IF EXISTS flights CASCADE;
DROP VIEW IF EXISTS inout24 CASCADE;
DROP VIEW IF EXISTS YYZflights CASCADE;

CREATE VIEW day AS
SELECT day::date as day FROM q5_parameters;
-- can get the given date using: (SELECT day from day)

CREATE VIEW n AS
SELECT n FROM q5_parameters;
-- can get the given number of flights using: (SELECT n from n)

-- list out the flights  
CREATE VIEW flights AS
SELECT id, outbound, inbound, s_dep, s_arv
FROM flight;

-- list of all available flights with a 24 hour interval (do a self join)
-- CREATE VIEW inOut24 AS
-- SELECT f1.inbound as airport, (f2.s_dep - f1.s_arv) as timeLeft, f2.outbound as outAirport
-- FROM flight f1 join flight f2 on f1.outbound = f2.inbound 
-- WHERE (f1.s_dep - f1.s_arv < '24:00:00') and (f2.s_dep - f1.s_arv > '00:00:00') and f2.outbound = f1.inbound and f1.id <> f2.id and f1.s_dep >= (SELECT day from day);

-- find all available flights from Toronto Pearson Airport
-- ASSUMPTION: any day on or after the day of interest is valid, because one can simply wait for the flight to leave eventually
CREATE VIEW YYZflights AS
SELECT id, outbound, inbound, s_dep, s_arv
FROM flights
WHERE outbound = 'YYZ' 
		and extract (year from s_dep) = (select extract (year from day) from day)
		and extract (month from s_dep) = (select extract (month from day) from day)
		and extract (day from s_dep) = (select extract (day from day) from day);

-- HINT: You can answer the question by writing one recursive query below, without any more views.
-- Your query that answers the question goes below the "insert into" line:
-- NOTE: we check whether the delay is greater than 00:00:00 because there is a chance that the plane has already departed by the time of arrival.
--       Therefore, we must filter out such cases where the buffer time is negative
INSERT INTO q5
WITH RECURSIVE hopping AS (
	(SELECT 1 as num_flights, inbound as destination, s_arv as arrivals, s_dep as dep FROM YYZflights)
	UNION all
	(SELECT num_flights + 1, inbound as destination, hopping.arrivals, s_dep as dep
	FROM hopping JOIN flights on hopping.destination = flights.outbound
	WHERE num_flights < (SELECT n from n) and s_dep - arrivals < '24:00:00' and s_dep - arrivals > '00:00:00')
) 
SELECT destination, num_flights
FROM hopping
ORDER BY num_flights;















