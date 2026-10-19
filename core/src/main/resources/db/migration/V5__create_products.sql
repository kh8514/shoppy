CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    price       INT          NOT NULL CHECK (price >= 0),
    stock       INT          NOT NULL CHECK (stock >= 0),
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);
