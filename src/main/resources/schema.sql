-- 起動時に毎回実行する（Hibernate の ddl-auto=update では直せないスキーマの調整）

-- 役職にパートを追加したため、Hibernate が以前作った「MANAGER / USER のみ」の制約を外す
ALTER TABLE IF EXISTS m_user DROP CONSTRAINT IF EXISTS accounts_role_check;
ALTER TABLE IF EXISTS m_user DROP CONSTRAINT IF EXISTS m_user_role_check;

-- 生年月日は扱わなくなったため、列を削除する
ALTER TABLE IF EXISTS m_user DROP COLUMN IF EXISTS birth_date;

-- 可能作業は DB の m_work_task / m_task_group で管理する（初期データ・旧 account_tasks からの引き継ぎは InitialTaskRunner が行う）

-- 確定を期間（16日〜翌15日）ごとから半月ごとに変えた（前半は16日、後半は翌月1日をキーにする）。
-- 以前に確定済みの期間は、前半・後半の両方を確定済みにする（一度だけ実行）
CREATE TABLE IF NOT EXISTS schema_migrations (name VARCHAR(100) PRIMARY KEY);

INSERT INTO t_period (period_start, confirmed, confirmed_at)
SELECT CAST(date_trunc('month', period_start) + INTERVAL '1 month' AS DATE), confirmed, confirmed_at
FROM t_period
WHERE confirmed AND EXTRACT(DAY FROM period_start) = 16
  AND NOT EXISTS (SELECT 1 FROM schema_migrations WHERE name = 'half_confirmation')
ON CONFLICT (period_start) DO NOTHING;

INSERT INTO schema_migrations (name) VALUES ('half_confirmation') ON CONFLICT (name) DO NOTHING;
