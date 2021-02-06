SET search_path TO World;
SELECT count(countrycode)
FROM countrylanguage 
WHERE countrylanguage = 'English';
