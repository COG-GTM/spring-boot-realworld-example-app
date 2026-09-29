-- Full-text index over articles (SQLite FTS5), kept in sync by triggers.
-- articles has a varchar primary key and an unstable implicit rowid, so article_search_ids
-- assigns each article a stable integer rowid in articles_fts.
create table article_search_ids (
  fts_rowid integer primary key autoincrement,
  article_id varchar(255) not null unique
);

create virtual table articles_fts using fts5(
  title,
  description,
  body,
  tokenize = 'porter unicode61 remove_diacritics 2'
);

insert into article_search_ids (article_id) select id from articles;

insert into articles_fts (rowid, title, description, body)
select S.fts_rowid, A.title, A.description, A.body
from articles A join article_search_ids S on S.article_id = A.id;

create trigger articles_fts_after_insert after insert on articles
begin
  insert into article_search_ids (article_id) values (new.id);
  insert into articles_fts (rowid, title, description, body)
  values (
    (select fts_rowid from article_search_ids where article_id = new.id),
    new.title,
    new.description,
    new.body);
end;

create trigger articles_fts_after_update after update of title, description, body on articles
begin
  update articles_fts
  set title = new.title, description = new.description, body = new.body
  where rowid = (select fts_rowid from article_search_ids where article_id = new.id);
end;

create trigger articles_fts_after_delete after delete on articles
begin
  delete from articles_fts
  where rowid = (select fts_rowid from article_search_ids where article_id = old.id);
  delete from article_search_ids where article_id = old.id;
end;
