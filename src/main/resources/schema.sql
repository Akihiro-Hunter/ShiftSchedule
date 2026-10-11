-- 起動時に毎回実行する（Hibernate の ddl-auto=update では直せないスキーマの調整）

-- 役職にパートを追加したため、Hibernate が以前作った「MANAGER / USER のみ」の制約を外す
ALTER TABLE IF EXISTS accounts DROP CONSTRAINT IF EXISTS accounts_role_check;

-- 可能作業の項目を変更したため、Hibernate が以前作った作業名の制約を外し、なくなった作業（苗穂・環状・光星）を消す
ALTER TABLE IF EXISTS account_tasks DROP CONSTRAINT IF EXISTS account_tasks_task_check;
DELETE FROM account_tasks WHERE task IN ('NAEBO', 'KANJO', 'KOSEI');
