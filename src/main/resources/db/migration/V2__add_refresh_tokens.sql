create table refresh_tokens (
  id varchar(255) primary key,
  user_id varchar(255) not null,
  token varchar(512) not null,
  created_at timestamp not null,
  expires_at timestamp not null,
  revoked boolean default false,
  foreign key (user_id) references users(id)
);
