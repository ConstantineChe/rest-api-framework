CREATE TABLE test_tbl (
       id serial,
       name text,
       data_1 text,
       include_id integer,
       external_include_id integer,
       lang json

       );

CREATE TABLE test_include_tbl (
       id serial,
       name text,
       data_include text
       );

CREATE TABLE test_external_include_tbl (
       id serial,
       name text,
       data_external text
       );

INSERT INTO test_tbl(id, name, data_1, include_id, external_include_id, lang) VALUES
       (1, 'test_n_1', 'test_data_1', 1, 1, '{"EN": "language_field_1", "RU": "языковое_поле_1"}'),
       (2, 'test_n_2', 'test_data_2', 2, 2, '{"EN": "language_field_2", "RU": "языковое_поле_2"}'),
       (3, 'test_n_3', 'test_data_3', 3, 3, '{"EN": "language_field_3", "RU": "языковое_поле_3"}'),
       (4, 'test_n_4', 'test_data_4', 4, 4, '{"EN": "language_field_4", "RU": "языковое_поле_4"}');

INSERT INTO test_include_tbl(id, name, data_include) VALUES
       (1, 'include_1', 'include_data_1'),
       (1, 'include_2', 'include_data_2'),
       (1, 'include_3', 'include_data_3'),
       (1, 'include_4', 'include_data_4');

INSERT INTO test_external_include_tbl(id, name, data_external) VALUES
       (1, 'external_include_1', 'external_include_data_1'),
       (1, 'external_include_1', 'external_include_data_1'),
       (1, 'external_include_1', 'external_include_data_1'),
       (1, 'external_include_1', 'external_include_data_1');
