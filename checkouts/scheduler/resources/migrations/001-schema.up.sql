CREATE TABLE schedule_tbl (
       id serial,
       scheduled_to timestamp,
       data json,
       executed_at timestamp,
       executed boolean
       );
