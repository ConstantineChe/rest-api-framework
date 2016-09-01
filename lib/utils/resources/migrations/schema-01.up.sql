CREATE TABLE test_tbl (
       id serial,
       name text,
       data_1 text
       );

CREATE TABLE test_include_tbl (
       id serial,
       name text,
       test integer,
       data_include text
       );

CREATE TABLE test_external_include_tbl (
       id serial,
       name text,
       test integer,
       data_external text
       );

INSERT INTO test_tbl(id, name, data_1) VALUES
       (1, 'test_n_1', 'test_data_1'),
       (2, 'test_n_2', 'test_data_2'),
       (3, 'test_n_3', 'test_data_3'),
       (4, 'test_n_4', 'test_data_4');
