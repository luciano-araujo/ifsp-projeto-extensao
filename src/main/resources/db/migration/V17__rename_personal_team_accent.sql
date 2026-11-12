-- V12 created personal teams named 'Meu Estudio' (no accent) for users without a name.
UPDATE teams SET name = 'Meu Estúdio' WHERE CAST(name AS BINARY) = 'Meu Estudio';
