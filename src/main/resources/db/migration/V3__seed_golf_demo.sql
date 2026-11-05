-- Demo data for the golf one-and-done contest.
-- Timestamps are stored as epoch milliseconds (UTC), matching how the app writes TIMESTAMP columns.

insert into golf_contests (id, name, season, created_at) values
  ('golf-contest-2026', '2026-27 One-and-Done', '2026-27', strftime('%s', '2026-01-01 00:00:00') * 1000);

insert into golf_golfers (id, name, country) values
  ('golfer-scheffler', 'Scottie Scheffler', 'USA'),
  ('golfer-mcilroy', 'Rory McIlroy', 'Northern Ireland'),
  ('golfer-schauffele', 'Xander Schauffele', 'USA'),
  ('golfer-morikawa', 'Collin Morikawa', 'USA'),
  ('golfer-aberg', 'Ludvig Aberg', 'Sweden'),
  ('golfer-fleetwood', 'Tommy Fleetwood', 'England'),
  ('golfer-hovland', 'Viktor Hovland', 'Norway'),
  ('golfer-cantlay', 'Patrick Cantlay', 'USA'),
  ('golfer-rahm', 'Jon Rahm', 'Spain'),
  ('golfer-fitzpatrick', 'Matt Fitzpatrick', 'England'),
  ('golfer-spieth', 'Jordan Spieth', 'USA'),
  ('golfer-thomas', 'Justin Thomas', 'USA');

insert into golf_tournaments (id, contest_id, name, course, starts_at, ends_at) values
  ('tourney-masters-2026', 'golf-contest-2026', 'Masters Tournament', 'Augusta National Golf Club', strftime('%s', '2026-04-09 11:00:00') * 1000, strftime('%s', '2026-04-12 23:59:59') * 1000),
  ('tourney-pga-2026', 'golf-contest-2026', 'PGA Championship', 'Aronimink Golf Club', strftime('%s', '2026-05-14 11:00:00') * 1000, strftime('%s', '2026-05-17 23:59:59') * 1000),
  ('tourney-usopen-2026', 'golf-contest-2026', 'U.S. Open', 'Shinnecock Hills Golf Club', strftime('%s', '2026-06-18 11:00:00') * 1000, strftime('%s', '2026-06-21 23:59:59') * 1000),
  ('tourney-open-2026', 'golf-contest-2026', 'The Open Championship', 'Royal Birkdale Golf Club', strftime('%s', '2026-07-16 11:00:00') * 1000, strftime('%s', '2026-07-19 23:59:59') * 1000),
  ('tourney-open-2027', 'golf-contest-2026', 'The Open Championship', 'Old Course at St Andrews', strftime('%s', '2027-07-15 11:00:00') * 1000, strftime('%s', '2027-07-18 23:59:59') * 1000),
  ('tourney-tourchamp-2027', 'golf-contest-2026', 'TOUR Championship', 'East Lake Golf Club', strftime('%s', '2027-08-26 11:00:00') * 1000, strftime('%s', '2027-08-29 23:59:59') * 1000);

insert into golf_tournament_results (tournament_id, golfer_id, finish_position, prize_money) values
  ('tourney-masters-2026', 'golfer-scheffler', 1, 3600000),
  ('tourney-masters-2026', 'golfer-mcilroy', 2, 2160000),
  ('tourney-masters-2026', 'golfer-aberg', 3, 1360000),
  ('tourney-masters-2026', 'golfer-schauffele', 4, 960000),
  ('tourney-masters-2026', 'golfer-morikawa', 5, 800000),
  ('tourney-masters-2026', 'golfer-rahm', 6, 720000),
  ('tourney-masters-2026', 'golfer-fleetwood', 7, 670000),
  ('tourney-masters-2026', 'golfer-hovland', 8, 620000),
  ('tourney-masters-2026', 'golfer-cantlay', 9, 580000),
  ('tourney-masters-2026', 'golfer-thomas', 10, 540000),
  ('tourney-masters-2026', 'golfer-spieth', 11, 500000),
  ('tourney-masters-2026', 'golfer-fitzpatrick', 12, 460000),
  ('tourney-pga-2026', 'golfer-schauffele', 1, 3420000),
  ('tourney-pga-2026', 'golfer-scheffler', 2, 2052000),
  ('tourney-pga-2026', 'golfer-morikawa', 3, 1292000),
  ('tourney-pga-2026', 'golfer-hovland', 4, 912000),
  ('tourney-pga-2026', 'golfer-fleetwood', 5, 760000),
  ('tourney-pga-2026', 'golfer-mcilroy', 6, 684000),
  ('tourney-pga-2026', 'golfer-cantlay', 7, 636500),
  ('tourney-pga-2026', 'golfer-rahm', 8, 589000),
  ('tourney-pga-2026', 'golfer-aberg', 9, 551000),
  ('tourney-pga-2026', 'golfer-fitzpatrick', 10, 513000),
  ('tourney-pga-2026', 'golfer-thomas', 11, 475000),
  ('tourney-pga-2026', 'golfer-spieth', 12, 437000),
  ('tourney-usopen-2026', 'golfer-rahm', 1, 4320000),
  ('tourney-usopen-2026', 'golfer-fitzpatrick', 2, 2592000),
  ('tourney-usopen-2026', 'golfer-mcilroy', 3, 1632000),
  ('tourney-usopen-2026', 'golfer-scheffler', 4, 1152000),
  ('tourney-usopen-2026', 'golfer-cantlay', 5, 960000),
  ('tourney-usopen-2026', 'golfer-aberg', 6, 864000),
  ('tourney-usopen-2026', 'golfer-schauffele', 7, 804000),
  ('tourney-usopen-2026', 'golfer-spieth', 8, 744000),
  ('tourney-usopen-2026', 'golfer-morikawa', 9, 696000),
  ('tourney-usopen-2026', 'golfer-hovland', 10, 648000),
  ('tourney-usopen-2026', 'golfer-fleetwood', 11, 600000),
  ('tourney-usopen-2026', 'golfer-thomas', 12, 552000),
  ('tourney-open-2026', 'golfer-fleetwood', 1, 3420000),
  ('tourney-open-2026', 'golfer-mcilroy', 2, 2052000),
  ('tourney-open-2026', 'golfer-schauffele', 3, 1292000),
  ('tourney-open-2026', 'golfer-rahm', 4, 912000),
  ('tourney-open-2026', 'golfer-scheffler', 5, 760000),
  ('tourney-open-2026', 'golfer-spieth', 6, 684000),
  ('tourney-open-2026', 'golfer-thomas', 7, 636500),
  ('tourney-open-2026', 'golfer-morikawa', 8, 589000),
  ('tourney-open-2026', 'golfer-fitzpatrick', 9, 551000),
  ('tourney-open-2026', 'golfer-aberg', 10, 513000),
  ('tourney-open-2026', 'golfer-hovland', 11, 475000),
  ('tourney-open-2026', 'golfer-cantlay', 12, 437000);

-- Demo users golfdemo1..3, password: password (BCrypt, same encoder the app uses).
-- Skipped if the username/email is already taken; entries and picks below only attach to demo users that exist.
insert or ignore into users (id, username, password, email, bio, image) values
  ('user-golfdemo1', 'golfdemo1', '$2a$10$/4JwdrtxLbXmx1rngB2CNuCLomOkoUqFeQgGit.NLnJjpnmvTsiBO', 'golfdemo1@example.com', 'Golf one-and-done demo player 1', 'https://static.productionready.io/images/smiley-cyrus.jpg'),
  ('user-golfdemo2', 'golfdemo2', '$2a$10$/4JwdrtxLbXmx1rngB2CNuCLomOkoUqFeQgGit.NLnJjpnmvTsiBO', 'golfdemo2@example.com', 'Golf one-and-done demo player 2', 'https://static.productionready.io/images/smiley-cyrus.jpg'),
  ('user-golfdemo3', 'golfdemo3', '$2a$10$/4JwdrtxLbXmx1rngB2CNuCLomOkoUqFeQgGit.NLnJjpnmvTsiBO', 'golfdemo3@example.com', 'Golf one-and-done demo player 3', 'https://static.productionready.io/images/smiley-cyrus.jpg');

insert into golf_contest_entries (contest_id, user_id, created_at)
select v.column1, v.column2, v.column3 from (values
  ('golf-contest-2026', 'user-golfdemo1', strftime('%s', '2026-03-01 12:00:00') * 1000),
  ('golf-contest-2026', 'user-golfdemo2', strftime('%s', '2026-03-02 12:00:00') * 1000),
  ('golf-contest-2026', 'user-golfdemo3', strftime('%s', '2026-03-03 12:00:00') * 1000)
) v join users u on u.id = v.column2;

insert into golf_picks (id, contest_id, tournament_id, user_id, golfer_id, created_at)
select v.column1, v.column2, v.column3, v.column4, v.column5, v.column6 from (values
  ('pick-golfdemo1-masters-2026', 'golf-contest-2026', 'tourney-masters-2026', 'user-golfdemo1', 'golfer-scheffler', strftime('%s', '2026-04-09 09:00:00') * 1000),
  ('pick-golfdemo1-pga-2026', 'golf-contest-2026', 'tourney-pga-2026', 'user-golfdemo1', 'golfer-morikawa', strftime('%s', '2026-05-14 09:00:00') * 1000),
  ('pick-golfdemo1-usopen-2026', 'golf-contest-2026', 'tourney-usopen-2026', 'user-golfdemo1', 'golfer-mcilroy', strftime('%s', '2026-06-18 09:00:00') * 1000),
  ('pick-golfdemo1-open-2026', 'golf-contest-2026', 'tourney-open-2026', 'user-golfdemo1', 'golfer-fleetwood', strftime('%s', '2026-07-16 09:00:00') * 1000),
  ('pick-golfdemo2-masters-2026', 'golf-contest-2026', 'tourney-masters-2026', 'user-golfdemo2', 'golfer-mcilroy', strftime('%s', '2026-04-09 09:00:00') * 1000),
  ('pick-golfdemo2-pga-2026', 'golf-contest-2026', 'tourney-pga-2026', 'user-golfdemo2', 'golfer-schauffele', strftime('%s', '2026-05-14 09:00:00') * 1000),
  ('pick-golfdemo2-usopen-2026', 'golf-contest-2026', 'tourney-usopen-2026', 'user-golfdemo2', 'golfer-fitzpatrick', strftime('%s', '2026-06-18 09:00:00') * 1000),
  ('pick-golfdemo2-open-2026', 'golf-contest-2026', 'tourney-open-2026', 'user-golfdemo2', 'golfer-spieth', strftime('%s', '2026-07-16 09:00:00') * 1000),
  ('pick-golfdemo3-masters-2026', 'golf-contest-2026', 'tourney-masters-2026', 'user-golfdemo3', 'golfer-aberg', strftime('%s', '2026-04-09 09:00:00') * 1000),
  ('pick-golfdemo3-pga-2026', 'golf-contest-2026', 'tourney-pga-2026', 'user-golfdemo3', 'golfer-hovland', strftime('%s', '2026-05-14 09:00:00') * 1000),
  ('pick-golfdemo3-usopen-2026', 'golf-contest-2026', 'tourney-usopen-2026', 'user-golfdemo3', 'golfer-cantlay', strftime('%s', '2026-06-18 09:00:00') * 1000),
  ('pick-golfdemo3-open-2026', 'golf-contest-2026', 'tourney-open-2026', 'user-golfdemo3', 'golfer-thomas', strftime('%s', '2026-07-16 09:00:00') * 1000)
) v join users u on u.id = v.column4;
