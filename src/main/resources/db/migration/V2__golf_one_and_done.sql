create table golf_contests (
  id varchar(255) primary key,
  name varchar(255) not null,
  season varchar(32) not null,
  created_at TIMESTAMP not null
);

create table golf_golfers (
  id varchar(255) primary key,
  name varchar(255) not null unique,
  country varchar(64)
);

create table golf_tournaments (
  id varchar(255) primary key,
  contest_id varchar(255) not null,
  name varchar(255) not null,
  course varchar(255),
  starts_at TIMESTAMP not null,
  ends_at TIMESTAMP not null
);

create table golf_tournament_results (
  tournament_id varchar(255) not null,
  golfer_id varchar(255) not null,
  finish_position integer,
  prize_money bigint not null default 0,
  primary key(tournament_id, golfer_id)
);

create table golf_contest_entries (
  contest_id varchar(255) not null,
  user_id varchar(255) not null,
  created_at TIMESTAMP not null,
  primary key(contest_id, user_id)
);

create table golf_picks (
  id varchar(255) primary key,
  contest_id varchar(255) not null,
  tournament_id varchar(255) not null,
  user_id varchar(255) not null,
  golfer_id varchar(255) not null,
  created_at TIMESTAMP not null,
  unique(contest_id, user_id, tournament_id),
  unique(contest_id, user_id, golfer_id)
);
