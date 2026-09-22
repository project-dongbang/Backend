ALTER TABLE receipts
    ADD COLUMN item_title VARCHAR(100),
    ADD COLUMN category VARCHAR(50),
    ADD COLUMN payment_method VARCHAR(50),
    ADD COLUMN memo TEXT,
    ADD COLUMN items_json TEXT NOT NULL DEFAULT '[]';
