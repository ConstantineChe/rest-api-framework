drop table if exists business_features_tbl;

-- sequence: seq_bft_id_pk {{{
drop sequence if exists seq_bft_id_pk;
create sequence seq_bft_id_pk
  start with 1
  increment by 1
  no maxvalue
  no minvalue
  cache 1;
-- }}}

create table business_features_tbl ( --{{{
  bft_id_pk       dm_pk default nextval('seq_bft_id_pk'),
  bft_bsn_id_fk   dm_ref_nn,
  bft_psf_id_fk   dm_ref,
  bft_type        dm_enum_nn,
  bft_description dm_text_medium,
  bft_lock        dm_bool_false,

  constraint PK_BUSINESS_FEATURES_TBL primary key (bft_id_pk)
);
-- }}}


drop table if exists features_tbl cascade;

-- sequence: seq_ftr_id_pk {{{
drop sequence if exists seq_ftr_id_pk;
create sequence seq_ftr_id_pk
  start with 1
  increment by 1
  no maxvalue
  no minvalue
  cache 1;
-- }}}

create table features_tbl ( -- {{{
  ftr_id_pk             dm_pk default nextval('seq_ftr_id_pk'),
  ftr_type              dm_enum_nn,
  ftr_name              dm_text_lang_nn,
  ftr_description       dm_text_lang,
  ftr_for_garages       dm_bool_false,
  ftr_for_car_washes    dm_bool_false,
  ftr_for_tire_stations dm_bool_false,
  ftr_order             dm_order default currval('seq_ftr_id_pk'::regclass),
  ftr_enabled           dm_bool_true,

  constraint PK_FEATURES_TBL primary key (ftr_id_pk)
);
-- }}}

alter table features_tbl add column ftr_important_for_garages dm_bool_false;
alter table features_tbl add column ftr_important_for_carwashes dm_bool_false;
alter table features_tbl add column ftr_important_for_tirestations dm_bool_false;
alter table features_tbl add column ftr_category dm_enum; -- comfort, payment, additional
alter table features_tbl add column ftr_languages dm_languages;
