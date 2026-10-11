-- 起動時に毎回実行する（Hibernate の ddl-auto=update では直せないスキーマの調整）

-- 役職にパートを追加したため、Hibernate が以前作った「MANAGER / USER のみ」の制約を外す
ALTER TABLE IF EXISTS accounts DROP CONSTRAINT IF EXISTS accounts_role_check;
