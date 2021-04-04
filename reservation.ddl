-- Air Travel Schema.

drop schema if exists reservation cascade;
create schema reservation;
set search_path to reservation;

-- ====================== Constraints ======================
-- enforced constraints: 
-- rating and age are numbers between 0 and 5 inclusive
-- age is a number greater than 0

-- foreign key constraints:
-- In table Booking there are 2 foreign keys: sID from Skipper, and cID from Craft

-- primary key constraints: 
-- In tables Skipper and Craft, the primary keys are sID and cID respectively

-- Not null constraints: 
-- in Skipper -> sName, rating, and age cannot be null
-- in Craft -> cName, and length cannot be null

-- Additional constraints: 
-- length in craft must be greater than 0

-- ====================== FDs ======================
-- Functional Dependencies
-- {sID -> sName, rating, age; cID -> cName, length) 

-- note: There is a redundancy in the table Booking where sID and cID repeat.
--       (explain)

CREATE TABLE Skipper (
    sID INT PRIMARY KEY,
    sName CHAR(30) NOT NULL,
    rating INT NOT NULL,
    age INT NOT NULL,
    check (rating >= 0 and rating <= 5),
    check (age > 0) 
); 

CREATE TABLE Craft (
    cID INT PRIMARY KEY,
    cName CHAR(30) NOT NULL,
    length INT NOT NULL,
    check (length > 0)
); 

CREATE TABLE Booking (
    sID INT PRIMARY KEY REFERENCES Skipper,
    cID INT PRIMARY KEY REFERENCES Craft,
    date timestamp NOT NULL
);