create table refresh_tokens (
  id varchar(255) primary key,
  user_id varchar(255) not null,
  token varchar(255) not null,
  expiry_date timestamp not null,
  revoked boolean not null default false,
  created_at timestamp not null default current_timestamp
);

create unique index idx_refresh_tokens_token on refresh_tokens(token);
create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
