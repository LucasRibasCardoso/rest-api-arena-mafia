-- Inserir regras de preço
INSERT INTO tb_price_rules (id, name, start_time, end_time, price, priority, is_active, is_default, created_at)
VALUES ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'Horário Noturno Semana', '18:00:00'::time, '00:00:00'::time, 50.00, 10, true, false, now()),
       ('5c6d7e8f-9a0b-4c1d-2e3f-4a5b6c7d8e9f'::uuid, 'Final de Semana', '08:00:00'::time, '00:00:00'::time, 85.00, 20, true, false, now());

-- Dias para a regra 1 (Segunda a Sexta - Horário Noturno)
INSERT INTO tb_price_rule_days (price_rule_id, day_of_week)
VALUES ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'MONDAY'),
       ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'TUESDAY'),
       ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'WEDNESDAY'),
       ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'THURSDAY'),
       ('7d8e9f1a-2b3c-4d5e-8f6a-9b7c8d1e2f3a'::uuid, 'FRIDAY');

-- Dias para a regra 2 (Sábado e Domingo - Final de Semana)
INSERT INTO tb_price_rule_days (price_rule_id, day_of_week)
VALUES ('5c6d7e8f-9a0b-4c1d-2e3f-4a5b6c7d8e9f'::uuid, 'SATURDAY'),
       ('5c6d7e8f-9a0b-4c1d-2e3f-4a5b6c7d8e9f'::uuid, 'SUNDAY');

