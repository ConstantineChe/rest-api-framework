set client_min_messages = 'warning';


drop table if exists gallery_tbl;

-- sequence: seq_glr_id_pk {{{
drop sequence if exists seq_glr_id_pk;
create sequence seq_glr_id_pk
  start with 1
  increment by 1
  no maxvalue
  no minvalue
  cache 1;
-- }}}

create table gallery_tbl ( --{{{
  glr_id_pk     integer default nextval('seq_glr_id_pk'),
  glr_bsn_id_fk integer,
  glr_type      text, -- photo, logo, background
  glr_name      json,
  glr_image     text,
  glr_order     integer default currval('seq_glr_id_pk'),
  glr_enabled   boolean,

  constraint PK_GALLERY_TBL primary key (glr_id_pk)
);
-- }}}



set client_min_messages = 'notice';
