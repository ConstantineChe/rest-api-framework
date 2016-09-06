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
  bft_id_pk       integer default nextval('seq_bft_id_pk'),
  bft_bsn_id_fk   integer,
  bft_psf_id_fk   integer,
  bft_type        text,
  bft_description text,
  bft_lock        boolean,

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
  ftr_id_pk             integer default nextval('seq_ftr_id_pk'),
  ftr_type              text,
  ftr_name              json,
  ftr_description       json,
  ftr_for_garages       boolean,
  ftr_for_car_washes    boolean,
  ftr_for_tire_stations boolean,
  ftr_order             integer default currval('seq_ftr_id_pk'),
  ftr_enabled           boolean,

  constraint PK_FEATURES_TBL primary key (ftr_id_pk)
);
-- }}}

alter table features_tbl add column ftr_important_for_garages boolean;
alter table features_tbl add column ftr_important_for_carwashes boolean;
alter table features_tbl add column ftr_important_for_tirestations boolean;
alter table features_tbl add column ftr_category text; -- comfort, payment, additional
alter table features_tbl add column ftr_languages jsonb;
