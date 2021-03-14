-- Q3. North and South Connections

-- You must not change the next 2 lines or the table definition.
SET SEARCH_PATH TO air_travel, public;
DROP TABLE IF EXISTS q3 CASCADE;

CREATE TABLE q3 (
    outbound VARCHAR(30),
    inbound VARCHAR(30),
    direct INT,
    one_con INT,
    two_con INT,
    earliest timestamp
);

-- Do this for each of the views that define your intermediate steps.  
-- (But give them better names!) The IF EXISTS avoids generating an error 
-- the first time this file is imported.
DROP VIEW IF EXISTS intermediate_step CASCADE;
DROP VIEW IF EXISTS canadaUSA CASCADE;
DROP VIEW IF EXISTS aprilOutbound CASCADE;
DROP VIEW IF EXISTS aprilInbound CASCADE;
DROP VIEW IF EXISTS aprilFlights CASCADE;
DROP VIEW IF EXISTS directFlights CASCADE;
DROP VIEW IF EXISTS oneLayover CASCADE;
DROP VIEW IF EXISTS twoLayover CASCADE;


-- Define views for your intermediate steps here:

-- find all combinations of flights between CANADA and the US from 
CREATE VIEW canadaUSA as
SELECT distinct a1.code as outPort, a2.code as inPort
FROM Airport a1, Airport a2
WHERE (a1.country = 'Canada' and a2.country = 'USA') or (a1.country = 'USA' and a2.country = 'Canada');

-- find outbound flights on 2021-04-30 
CREATE VIEW aprilOutbound as
SELECT flight.id as id, flight.outbound as outAir, airport.country, s_dep as departure
FROM flight JOIN airport on flight.outbound = airport.code 
WHERE date(flight.s_dep) = '2021-04-30';

-- find inbound flights on 2021-04-30 
CREATE VIEW aprilInbound as
SELECT flight.id as id, flight.inbound as inAir, airport.country, s_arv as arrival
FROM flight JOIN airport on flight.inbound = airport.code 
WHERE date(flight.s_arv) = '2021-04-30';

-- combine inbound and outbound tables
CREATE VIEW aprilFlights as
SELECT aprilOutbound.id as id, outair, inair, aprilOutbound.country as outCountry, aprilInbound.country as inCountry, aprilOutbound.departure, aprilInbound.arrival
FROM aprilOutbound JOIN aprilInbound on aprilOutbound.id = aprilInbound.id;

-- find direct routes
CREATE VIEW directFlights as 
SELECT distinct aprilFlights.outair as outPort, aprilFlights.inair as inPort, aprilFlights.arrival
FROM aprilFlights JOIN canadaUSA on aprilFlights.outair = canadaUSA.outPort and aprilFlights.inair = canadaUSA.inPort;

-- find one connection
CREATE VIEW oneLayover as
SELECT distinct canadaUSA.outPort, canadaUSA.inPort, f2.arrival 
FROM aprilFlights f1 JOIN canadaUSA on f1.outair = canadaUSA.outPort 
                     JOIN aprilFlights f2 on f1.inair = f2.outair and f2.inair = canadaUSA.inPort
WHERE f2.departure - f1.arrival >= '00:30:00';

-- find two connections
CREATE VIEW twoLayover as
SELECT distinct canadaUSA.outPort, canadaUSA.inPort, f3.arrival
FROM aprilFlights f1 JOIN canadaUSA on f1.outair = canadaUSA.outPort 
                     JOIN aprilFlights f2 on f1.inair = f2.outair
                     JOIN aprilFlights f3 on f2.inair = f3.outair and f3.inair = canadaUSA.inPort
WHERE f2.departure - f1.arrival >= '00:30:00' and f3.departure - f2.arrival >= '00:30:00';

-- TODO: combine the tables
CREATE VIEW combined as
(SELECT outPort, inPort, 1 as direct, 0 as one_con, 0 as two_con, arrival FROM directFlights) UNION 
(SELECT outPort, inPort, 0 as direct, 1 as one_con, 0 as two_con, arrival FROM oneLayover) UNION 
(SELECT outPort, inPort, 0 as direct, 0 as one_con, 1 as two_con, arrival FROM twoLayover);

CREATE VIEW nonFlights as 
(SELECT outport, inport FROM canadaUSA) EXCEPT
(SELECT outPort, inPort FROM combined GROUP BY outport, inPort);

CREATE VIEW nulls as 
SELECT outPort, inPort, 0 as direct, 0 as one_con, 0 as two_con, null as arrival 
FROM nonFlights;

CREATE VIEW resultPairs as
(SELECT combined.outPort, combined.inPort, direct, one_con, two_con, arrival
FROM canadaUSA c1 FULL JOIN combined on c1.outport = combined.outPort and c1.inPort = combined.inPort);

CREATE VIEW result as
(SELECT outPort, inPort, direct, one_con, two_con
FROM combined) UNION 
(SELECT outPort, inPort, direct, one_con, two_con
FROM nulls); 


-- (SELECT a1.city as outPort, a2.city as inPort, direct, one_con, two_con
-- FROM combined JOIN airport a1 on a1.code = combined.outPort
--               JOIN airport a2 on a2.code = combined.inPort) 

CREATE VIEW resultArrivals as
SELECT result.outPort, result.inPort, result.direct, result.one_con, result.two_con, combined.arrival
FROM result LEFT join combined on result.outport = combined.outport and result.inport = combined.inport
                                                               and result.direct = combined.direct
                                                               and result.one_con = combined.one_con
                                                               and result.two_con = combined.two_con;

-- Your query that answers the question goes below the "insert into" line:
INSERT INTO q3
SELECT outport, inPort, sum(direct) as direct, sum(one_con) as one_con, sum(two_con) as two_con, min(arrival)
FROM resultArrivals
GROUP BY outport, inPort;