-- Usuarios de demonstracao (senha Teste@123). Dominio .test: nenhum e-mail real e envolvido.
INSERT IGNORE INTO users (email, password, name) VALUES
('ana.admin@creatorworkbench.test', '$2a$10$ezeOSqVguD0LQe77yUcfcOcdyALnGM81r.0zSk/dlX22Fn9OFiu3i', 'Ana Admin'),
('bia.criadora@creatorworkbench.test', '$2a$10$ezeOSqVguD0LQe77yUcfcOcdyALnGM81r.0zSk/dlX22Fn9OFiu3i', 'Bia Criadora'),
('rui.revisor@creatorworkbench.test', '$2a$10$ezeOSqVguD0LQe77yUcfcOcdyALnGM81r.0zSk/dlX22Fn9OFiu3i', 'Rui Revisor'),
('admin.sistema@creatorworkbench.test', '$2a$10$ezeOSqVguD0LQe77yUcfcOcdyALnGM81r.0zSk/dlX22Fn9OFiu3i', 'Admin do Sistema');
