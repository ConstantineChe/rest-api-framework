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
       enabled boolean,
       CONSTRAINT unique_email UNIQUE(email)
       );
