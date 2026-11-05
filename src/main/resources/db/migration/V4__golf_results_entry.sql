-- Prize money credited to each pick once its tournament's results are recorded.
-- NULL means the tournament has no results yet (pick is pending).
alter table golf_picks add column prize_money bigint;

update golf_picks
set prize_money = coalesce(
  (select R.prize_money from golf_tournament_results R
    where R.tournament_id = golf_picks.tournament_id and R.golfer_id = golf_picks.golfer_id), 0)
where exists (
  select 1 from golf_tournament_results R where R.tournament_id = golf_picks.tournament_id);

-- Demo admin allowed to record tournament results (see golf.admin-user-ids), password: password.
insert or ignore into users (id, username, password, email, bio, image) values
  ('user-golfadmin', 'golfadmin', '$2a$10$/4JwdrtxLbXmx1rngB2CNuCLomOkoUqFeQgGit.NLnJjpnmvTsiBO', 'golfadmin@example.com', 'Golf one-and-done results admin', 'https://static.productionready.io/images/smiley-cyrus.jpg');
