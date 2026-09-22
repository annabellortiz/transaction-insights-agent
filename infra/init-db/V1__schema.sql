-- V1__schema.sql

CREATE TABLE IF NOT EXISTS accounts (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  external_id TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS categories (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  parent_id BIGINT REFERENCES categories(id),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS transactions (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT REFERENCES accounts(id) NOT NULL,
  category_id BIGINT REFERENCES categories(id),
  txn_date DATE NOT NULL,
  amount NUMERIC(14,2) NOT NULL,
  currency CHAR(3) DEFAULT 'USD',
  merchant TEXT,
  description TEXT,
  imported_row JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS staging_transactions (
  id BIGSERIAL PRIMARY KEY,
  raw_row JSONB NOT NULL,
  parsed BOOLEAN DEFAULT FALSE,
  error TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_transactions_txn_date ON transactions(txn_date);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_transactions_amount ON transactions(amount);
CREATE INDEX IF NOT EXISTS idx_transactions_merchant ON transactions(merchant);
CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(account_id);
