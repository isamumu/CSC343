SET search_path TO World;
SELECT count(countrylanguage)
FROM country JOIN countrylanguage on code=countrycode 
WHERE name = 'Mozambique';
