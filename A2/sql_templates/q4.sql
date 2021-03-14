-- Q4. Plane Capacity Histogram

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q4 CASCADE;

CREATE TABLE q4 (
	airline CHAR(2),
	tail_number CHAR(5),
	very_low INT,
	low INT,
	fair INT,
	normal INT,
	high INT
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS airlineTail cascade;
DROP VIEW IF EXISTS departed cascade;
DROP VIEW IF EXISTS groupings cascade;

-- Define views for your intermediate steps here:

-- relate airline and tail number
CREATE VIEW airlineTail as
SELECT airline, tail_number, (capacity_economy + capacity_business + capacity_first) as maxCap
FROM plane;

-- find planes that have actually departed
CREATE VIEW departed as
SELECT id, airline, plane
FROM flight JOIN departure on flight.id = departure.flight_id;

-- find the capacity of flihgts that have actually departed
CREATE VIEW capDepartedFlights as
SELECT id, airlineTail.airline, airlineTail.tail_number, maxcap
FROM airlineTail JOIN departed on airlineTail.airline = departed.airline and airlineTail.tail_number = departed.plane;

CREATE VIEW nonDepartedFlights as 
(SELECT airlineTail.airline, airlineTail.tail_number FROM airlineTail) EXCEPT (SELECT capDepartedFlights.airline, capDepartedFlights.tail_number FROM capDepartedFlights);

-- flight groupings
CREATE VIEW groupings as 
SELECT flight_id, count(id) as num_passengers
FROM booking
GROUP BY flight_id;

-- relate max capacity, and current capacity
CREATE VIEW groupPlanes as 
SELECT flight_id, num_passengers, airline, tail_number, (num_passengers * 100 / maxcap) as percent
FROM groupings JOIN capDepartedFlights on groupings.flight_id = capDepartedFlights.id;

-- relate very_low capacity planes (0% - 20%)
CREATE VIEW verylowCap as
SELECT airline, tail_number, 1 as very_low
FROM groupPlanes
WHERE percent >= 0 and percent < 20;

-- relate low capacity planes (20% - 40%)
CREATE VIEW lowCap as
SELECT airline, tail_number, 1 as low
FROM groupPlanes
WHERE percent >= 20 and percent < 40;

-- relate fair capacity planes (40% - 60%)
CREATE VIEW fairCap as
SELECT airline, tail_number, 1 as fair
FROM groupPlanes
WHERE percent >= 40 and percent < 60;

-- relate normal capacity planes (60% - 80%)
CREATE VIEW normalCap as
SELECT airline, tail_number, 1 as normal
FROM groupPlanes
WHERE percent >= 60 and percent < 80;

-- relate high capacity planes (80% or more)
CREATE VIEW highCap as
SELECT airline, tail_number, 1 as high
FROM groupPlanes
WHERE percent >= 80;

CREATE VIEW combinedFlights as
(SELECT nonDepartedFlights.airline, nonDepartedFlights.tail_number, 0 as very_low, 0 as low, 0 as fair, 0 as normal, 0 as high FROM nonDepartedFlights) UNION
(SELECT verylowCap.airline, verylowCap.tail_number, very_low, 0 as low, 0 as fair, 0 as normal, 0 as high FROM verylowCap) UNION
(SELECT lowCap.airline, lowCap.tail_number, 0 as very_low, low, 0 as fair, 0 as normal, 0 as high FROM lowCap) UNION
(SELECT fairCap.airline, fairCap.tail_number, 0 as very_low, 0 as low, fair, 0 as normal, 0 as high FROM fairCap) UNION
(SELECT normalCap.airline, normalCap.tail_number, 0 as very_low, 0 as low, 0 as fair, normal, 0 as high FROM normalCap) UNION
(SELECT highCap.airline, highCap.tail_number, 0 as very_low, 0 as low, 0 as fair, 0 as normal, high FROM highCap);  

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q4
SELECT combinedFlights.airline, combinedFlights.tail_number, sum(very_low) as very_low, sum(low) as low, sum(fair) as fair, sum(normal) as normal, sum(high) as high
FROM combinedFlights
GROUP BY combinedFlights.airline, combinedFlights.tail_number;