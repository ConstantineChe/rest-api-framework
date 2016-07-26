CREATE TYPE user_status AS ENUM ('basic', 'vip');
CREATE TYPE user_gender AS ENUM ('male', 'female');

CREATE TABLE users_tbl (
       id serial,
       name text,
       surname text,
       middlename text,
       email text,
       password text,
       registration_date date,
       gender user_gender,
       phones json,
       status user_status DEFAULT 'basic',
       dob date,
       enabled boolean
       );

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

CREATE TABLE vehicles_modifications (
       id serial,
       model_id integer,
       name text,
       made_from date,
       made_until date,
       enabled boolean
       );
