-- Usuarios de demonstracao. A senha NAO fica no repositorio: o hash BCrypt chega na variavel @demo_hash
-- (seed.sh a define antes deste arquivo, a partir de DEMO_PASSWORD em .state/secrets.env).
-- Dominio .test: nenhum e-mail real e envolvido.
INSERT IGNORE INTO users (email, password, name) VALUES
('ana.admin@creatorworkbench.test', @demo_hash, 'Ana Admin'),
('bia.criadora@creatorworkbench.test', @demo_hash, 'Bia Criadora'),
('rui.revisor@creatorworkbench.test', @demo_hash, 'Rui Revisor'),
('admin.sistema@creatorworkbench.test', @demo_hash, 'Admin do Sistema');
