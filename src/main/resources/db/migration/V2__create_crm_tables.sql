create table lps (
  id varchar(255) primary key,
  user_id varchar(255) not null,
  name varchar(255),
  company varchar(255),
  email varchar(255),
  stage varchar(255) not null,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create table activities (
  id varchar(255) primary key,
  lp_id varchar(255) not null,
  user_id varchar(255) not null,
  type varchar(255),
  notes text,
  created_at TIMESTAMP NOT NULL
);

create table lp_relationships (
  lp_id varchar(255) not null,
  contact_id varchar(255) not null,
  role varchar(255),
  primary key(lp_id, contact_id)
);
