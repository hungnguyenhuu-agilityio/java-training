--liquibase formatted sql

--changeset ecommerce:001-baseline-schema
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE refresh_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    replaced_by_id BIGINT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_refresh_sessions_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_sessions_replacement FOREIGN KEY (replaced_by_id) REFERENCES refresh_sessions (id) ON DELETE SET NULL
);
CREATE INDEX ix_refresh_sessions_user_family ON refresh_sessions (user_id, family_id);
CREATE INDEX ix_refresh_sessions_expiry ON refresh_sessions (expires_at);

CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(280) NOT NULL,
    description TEXT,
    sku VARCHAR(50) NOT NULL,
    price DECIMAL(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_products_slug UNIQUE (slug),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
);
CREATE INDEX ix_products_category_active ON products (category_id, is_active);
CREATE INDEX ix_products_name ON products (name);

CREATE TABLE warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_warehouses_code UNIQUE (code)
);

CREATE TABLE warehouse_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    safety_stock_threshold INT NOT NULL DEFAULT 0,
    is_low_stock BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_warehouse_inventory_location_product UNIQUE (warehouse_id, product_id),
    CONSTRAINT ck_warehouse_inventory_on_hand CHECK (on_hand_quantity >= 0),
    CONSTRAINT ck_warehouse_inventory_reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= on_hand_quantity),
    CONSTRAINT ck_warehouse_inventory_threshold CHECK (safety_stock_threshold >= 0),
    CONSTRAINT fk_warehouse_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_warehouse_inventory_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);
CREATE INDEX ix_warehouse_inventory_low_stock ON warehouse_inventory (warehouse_id, is_low_stock);

CREATE TABLE carts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_cart_items_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    order_code VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_amount DECIMAL(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    shipping_name VARCHAR(150) NOT NULL,
    shipping_phone VARCHAR(30) NOT NULL,
    shipping_address_line1 VARCHAR(255) NOT NULL,
    shipping_address_line2 VARCHAR(255),
    shipping_city VARCHAR(120) NOT NULL,
    shipping_region VARCHAR(120),
    shipping_postal_code VARCHAR(30),
    shipping_country_code CHAR(2) NOT NULL,
    note TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_orders_order_code UNIQUE (order_code),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_total CHECK (total_amount >= 0),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE RESTRICT
);
CREATE INDEX ix_orders_user_created ON orders (user_id, created_at);
CREATE INDEX ix_orders_status ON orders (status);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(50) NOT NULL,
    unit_price DECIMAL(14,2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    CONSTRAINT ck_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_total CHECK (total_price >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);
CREATE INDEX ix_order_items_order ON order_items (order_id);

CREATE TABLE order_status_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    note TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_status_histories_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_status_histories_actor FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX ix_order_status_histories_order_created ON order_status_histories (order_id, created_at);

CREATE TABLE inventory_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_inventory_reservations_order UNIQUE (order_id),
    CONSTRAINT ck_inventory_reservations_status CHECK (status IN ('ACTIVE', 'COMMITTED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT fk_inventory_reservations_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_reservations_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE RESTRICT
);
CREATE INDEX ix_inventory_reservations_expiry ON inventory_reservations (status, expires_at);

CREATE TABLE inventory_reservation_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uq_inventory_reservation_items_product UNIQUE (reservation_id, product_id),
    CONSTRAINT ck_inventory_reservation_items_quantity CHECK (quantity > 0),
    CONSTRAINT fk_inventory_reservation_items_reservation FOREIGN KEY (reservation_id) REFERENCES inventory_reservations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_reservation_items_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);

CREATE TABLE inventory_alert_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_inventory_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    last_alerted_at TIMESTAMP(6),
    recovered_at TIMESTAMP(6),
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_inventory_alert_states_inventory UNIQUE (warehouse_inventory_id),
    CONSTRAINT ck_inventory_alert_states_status CHECK (status IN ('NORMAL', 'LOW')),
    CONSTRAINT fk_inventory_alert_states_inventory FOREIGN KEY (warehouse_inventory_id) REFERENCES warehouse_inventory (id) ON DELETE CASCADE
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_session_id VARCHAR(255),
    provider_payment_id VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_payments_order UNIQUE (order_id),
    CONSTRAINT uq_payments_provider_session UNIQUE (provider, provider_session_id),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT
);
CREATE INDEX ix_payments_provider_payment ON payments (provider, provider_payment_id);

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    provider_refund_id VARCHAR(255),
    status VARCHAR(30) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    reason VARCHAR(255),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    CONSTRAINT uq_refunds_provider_refund UNIQUE (provider_refund_id),
    CONSTRAINT uq_refunds_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_refunds_amount CHECK (amount > 0),
    CONSTRAINT ck_refunds_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE RESTRICT
);
CREATE INDEX ix_refunds_payment ON refunds (payment_id);

CREATE TABLE processed_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    provider_object_id VARCHAR(255) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_processed_webhook_events_provider_event UNIQUE (provider, provider_event_id),
    CONSTRAINT ck_processed_webhook_events_status CHECK (status IN ('RECEIVED', 'PROCESSED', 'FAILED', 'IGNORED'))
);
CREATE INDEX ix_processed_webhook_events_status_created ON processed_webhook_events (status, created_at);

CREATE TABLE notification_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_type VARCHAR(50) NOT NULL,
    deduplication_key VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    delivered_at TIMESTAMP(6),
    CONSTRAINT uq_notification_requests_deduplication UNIQUE (deduplication_key),
    CONSTRAINT ck_notification_requests_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_notification_requests_status CHECK (status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'FAILED'))
);
CREATE INDEX ix_notification_requests_delivery ON notification_requests (status, next_attempt_at);

CREATE TABLE idempotency_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    response_status INT,
    response_body TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_idempotency_keys_actor_operation UNIQUE (user_id, request_path, idempotency_key),
    CONSTRAINT fk_idempotency_keys_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX ix_idempotency_keys_expiry ON idempotency_keys (expires_at);

--rollback DROP TABLE idempotency_keys;
--rollback DROP TABLE notification_requests;
--rollback DROP TABLE processed_webhook_events;
--rollback DROP TABLE refunds;
--rollback DROP TABLE payments;
--rollback DROP TABLE inventory_alert_states;
--rollback DROP TABLE inventory_reservation_items;
--rollback DROP TABLE inventory_reservations;
--rollback DROP TABLE order_status_histories;
--rollback DROP TABLE order_items;
--rollback DROP TABLE orders;
--rollback DROP TABLE cart_items;
--rollback DROP TABLE carts;
--rollback DROP TABLE warehouse_inventory;
--rollback DROP TABLE warehouses;
--rollback DROP TABLE products;
--rollback DROP TABLE categories;
--rollback DROP TABLE refresh_sessions;
--rollback DROP TABLE user_roles;
--rollback DROP TABLE users;
