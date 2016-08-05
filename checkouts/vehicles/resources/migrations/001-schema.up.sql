CREATE TABLE vehicles_tbl (
       id serial,
       make_id integer,
       model_id integer,
       year integer,
       modification_id integer,
       registration_number text,
       vin_code text,
       enabled boolean
       );

CREATE TABLE vehicle_makes_tbl (
       id serial,
       name text,
       enabled boolean
       );

CREATE TABLE vehicle_models_tbl (
       id serial,
       make_id integer,
       name text,
       enabled boolean
       );

CREATE TABLE vehicle_modifications_tbl (
       id serial,
       model_id integer,
       name text,
       made_from date,
       made_until date,
       enabled boolean
       );
