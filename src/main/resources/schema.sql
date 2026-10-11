-- 起動時に毎回実行する（Hibernate の ddl-auto=update では直せないスキーマの調整）

-- 役職にパートを追加したため、Hibernate が以前作った「MANAGER / USER のみ」の制約を外す
ALTER TABLE IF EXISTS accounts DROP CONSTRAINT IF EXISTS accounts_role_check;

-- 可能作業の項目を変更したため、Hibernate が以前作った作業名の制約を外し、なくなった作業（苗穂・環状・光星）を消す
ALTER TABLE IF EXISTS account_tasks DROP CONSTRAINT IF EXISTS account_tasks_task_check;
DELETE FROM account_tasks WHERE task IN ('NAEBO', 'KANJO', 'KOSEI');

-- 確定を期間（16日〜翌15日）ごとから半月ごとに変えた（前半は16日、後半は翌月1日をキーにする）。
-- 以前に確定済みの期間は、前半・後半の両方を確定済みにする（一度だけ実行）
CREATE TABLE IF NOT EXISTS schema_migrations (name VARCHAR(100) PRIMARY KEY);

INSERT INTO shift_periods (period_start, confirmed, confirmed_at)
SELECT CAST(date_trunc('month', period_start) + INTERVAL '1 month' AS DATE), confirmed, confirmed_at
FROM shift_periods
WHERE confirmed AND EXTRACT(DAY FROM period_start) = 16
  AND NOT EXISTS (SELECT 1 FROM schema_migrations WHERE name = 'half_confirmation')
ON CONFLICT (period_start) DO NOTHING;

INSERT INTO schema_migrations (name) VALUES ('half_confirmation') ON CONFLICT (name) DO NOTHING;
