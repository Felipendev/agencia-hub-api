-- Telefone do cliente é opcional: clientes de milhas ou leads iniciais podem
-- ser cadastrados apenas pelo nome. O índice existente já ignora telefone vazio
-- e valores NULL podem se repetir no PostgreSQL.
ALTER TABLE customers ALTER COLUMN phone DROP NOT NULL;
