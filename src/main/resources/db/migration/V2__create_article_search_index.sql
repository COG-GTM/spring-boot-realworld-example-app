-- Full-text index over articles (SQLite FTS5).
-- A standalone FTS table keyed by article_id is used instead of an external-content
-- table because articles has a varchar primary key and its implicit rowid is not stable.
create virtual table articles_fts using fts5(
  article_id UNINDEXED,
  title,
  description,
  body,
  tokenize = 'porter unicode61 remove_diacritics 2'
);

insert into articles_fts (article_id, title, description, body)
select id, title, description, body from articles;

create trigger articles_fts_after_insert after insert on articles
begin
  insert into articles_fts (article_id, title, description, body)
  values (new.id, new.title, new.description, new.body);
end;

create trigger articles_fts_after_update after update of title, description, body on articles
begin
  delete from articles_fts where article_id = old.id;
  insert into articles_fts (article_id, title, description, body)
  values (new.id, new.title, new.description, new.body);
end;

create trigger articles_fts_after_delete after delete on articles
begin
  delete from articles_fts where article_id = old.id;
end;
