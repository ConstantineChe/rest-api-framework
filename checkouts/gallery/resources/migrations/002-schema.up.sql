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
  glr_id_pk     dm_pk default nextval('seq_glr_id_pk'),
  glr_bsn_id_fk dm_ref_nn,
  glr_type      dm_smallenum_nn, -- photo, logo, background
  glr_name      dm_text_lang,
  glr_image     dm_image_nn,
  glr_order     dm_order default currval('seq_glr_id_pk'),
  glr_enabled   dm_bool_true,

  constraint PK_GALLERY_TBL primary key (glr_id_pk)
);
-- }}}
