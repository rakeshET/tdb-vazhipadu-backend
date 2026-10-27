ALTER TABLE orders ADD COLUMN cancelled_at TIMESTAMP(6) WITH TIME ZONE;

CREATE INDEX idx_orders_temple_created ON orders (temple_id, created_at);
