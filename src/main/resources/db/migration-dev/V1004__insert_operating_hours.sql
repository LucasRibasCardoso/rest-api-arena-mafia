INSERT INTO tb_operating_hours (id, start_time, end_time, is_active, created_at)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, '08:00:00'::time, '00:00:00'::time, true, now()),
       ('a8b3f5e2-9d1c-4a7b-8e6f-3c5d7a9b2e4f'::uuid, '08:00:00'::time, '00:00:00'::time, true, now());

-- Dias para o horário 1 (Segunda a Sexta)
INSERT INTO tb_operating_hours_days (operating_hours_id, day_of_week)
VALUES ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'MONDAY'),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'TUESDAY'),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'WEDNESDAY'),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'THURSDAY'),
       ('f47ac10b-58cc-4372-a567-0e02b2c3d479'::uuid, 'FRIDAY');

-- Dias para o horário 2 (Sábado e Domingo)
INSERT INTO tb_operating_hours_days (operating_hours_id, day_of_week)
VALUES ('a8b3f5e2-9d1c-4a7b-8e6f-3c5d7a9b2e4f'::uuid, 'SATURDAY'),
       ('a8b3f5e2-9d1c-4a7b-8e6f-3c5d7a9b2e4f'::uuid, 'SUNDAY');
