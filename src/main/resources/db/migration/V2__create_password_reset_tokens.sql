create table password_reset_tokens (
  id varchar(255) primary key,
  token varchar(255) UNIQUE not null,
  user_id varchar(255) not null,
  expires_at TIMESTAMP NOT NULL,
  used boolean NOT NULL DEFAULT 0
);
