alter table users add column role varchar(16) not null default 'USER';

alter table articles add column deleted_at TIMESTAMP;

create table article_reports (
  id varchar(255) primary key,
  article_id varchar(255) not null,
  reporter_id varchar(255) not null,
  reason varchar(32) not null,
  reporter_comment text,
  status varchar(16) not null default 'PENDING',
  moderator_id varchar(255),
  moderator_note text,
  created_at TIMESTAMP NOT NULL,
  resolved_at TIMESTAMP
);

create unique index ux_article_reports_article_reporter_pending
  on article_reports(article_id, reporter_id)
  where status = 'PENDING';

create index ix_article_reports_status_created_at
  on article_reports(status, created_at);

insert into users (id, username, email, password, bio, image, role) values (
  '00000000-0000-0000-0000-000000000001',
  'admin',
  'admin@realworld.dev',
  '$2y$10$LSRF2D.7tQBKcU.5dShnLOytsfpqtiRpzO3WWnuGcUkuNBD.Nghea',
  '',
  'https://static.productionready.io/images/smiley-cyrus.jpg',
  'ADMIN'
);
