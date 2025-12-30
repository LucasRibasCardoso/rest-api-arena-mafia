-- Inserir agendamentos (schedule_entries)
INSERT INTO tb_schedule_entries (id, court_id, date, start_time, end_time, entry_type, created_at) VALUES
       -- HOJE - Quadra A (Reservations)
       ('3e7a9b2c-5d8f-4c3e-9a1b-7f6e4d2c1a0b', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '08:00:00', '09:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('8f1c4e9a-2b7d-4a5c-8e3f-1d6b9c4a7e2f', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '10:00:00', '11:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('c2d5e8f1-9a4b-4c7e-8d3a-5f6b2e1c9a7d', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '14:00:00', '15:00:00', 'RESERVATION', CURRENT_TIMESTAMP),

       -- HOJE - Quadra A (BlockedTime - 1h específica)
       ('f1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '16:00:00', '17:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),

       -- HOJE - Quadra B (Reservations)
       ('b5c8e1f4-7a9d-4c2e-8b6f-3d5a9c7e1b4f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '08:30:00', '09:30:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('e9f2a5c8-1b4d-4c7e-8a3f-6d9b2c5e8a1f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '10:30:00', '11:30:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('a4c7e9f2-8b5d-4c1a-8e6f-3d2b9c7a5e1c', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '15:00:00', '16:00:00', 'RESERVATION', CURRENT_TIMESTAMP),

       -- HOJE - Quadra B (BlockedTime - final do dia)
       ('a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '18:00:00', '19:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),

       -- AMANHÃ - Quadra A (Reservations que conflitam com blocked time da manhã)
       ('1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 1, '09:00:00', '10:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 1, '10:30:00', '11:30:00', 'RESERVATION', CURRENT_TIMESTAMP),

       -- AMANHÃ - Quadra A (BlockedTime - manhã inteira)
       ('b3c4d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7e', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 1, '08:00:00', '12:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),

       -- AMANHÃ - Quadra B (Reservations que conflitam com blocked time da tarde)
       ('3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE + 1, '15:00:00', '16:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE + 1, '16:30:00', '17:30:00', 'RESERVATION', CURRENT_TIMESTAMP),

       -- AMANHÃ - Quadra B (BlockedTime - tarde inteira)
       ('c4d5e6f7-a8b9-4c0d-1e2f-3a4b5c6d7e8f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE + 1, '14:00:00', '18:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),

       -- PRÓXIMOS 3 DIAS - Quadra A (BlockedTimes recorrentes - manutenção programada)
       ('d5e6f7a8-b9c0-4d1e-2f3a-4b5c6d7e8f9a', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 2, '08:00:00', '10:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),
       ('e6f7a8b9-c0d1-4e2f-3a4b-5c6d7e8f9a0b', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 3, '08:00:00', '10:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP),
       ('f7a8b9c0-d1e2-4f3a-4b5c-6d7e8f9a0b1c', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE + 4, '08:00:00', '10:00:00', 'BLOCKED_TIME', CURRENT_TIMESTAMP);

-- Inserir detalhes das reservas
INSERT INTO tb_reservations (id, user_id, modality_id, scheduled_by_admin_id, price, status, recurring_reservation_id, cancelled_by_admin_id) VALUES
       -- HOJE - Quadra A
       ('3e7a9b2c-5d8f-4c3e-9a1b-7f6e4d2c1a0b', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440002', NULL, 80.00, 'CONFIRMED', NULL, NULL),
       ('8f1c4e9a-2b7d-4a5c-8e3f-1d6b9c4a7e2f', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440005', NULL, 75.00, 'CONFIRMED', NULL, NULL),
       ('c2d5e8f1-9a4b-4c7e-8d3a-5f6b2e1c9a7d', 'bce2c206-3b8d-4ce8-b05d-b236cc375d5e', '550e8400-e29b-41d4-a716-446655440006', '960289b9-d32d-4f00-8df7-02f4c04a017c', 90.00, 'CONFIRMED', NULL, NULL),

       -- HOJE - Quadra B
       ('b5c8e1f4-7a9d-4c2e-8b6f-3d5a9c7e1b4f', 'bce2c206-3b8d-4ce8-b05d-b236cc375d5e', '550e8400-e29b-41d4-a716-446655440002', NULL, 70.00, 'CONFIRMED', NULL, NULL),
       ('e9f2a5c8-1b4d-4c7e-8a3f-6d9b2c5e8a1f', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440006', NULL, 65.00, 'COMPLETED', NULL, NULL),
       ('a4c7e9f2-8b5d-4c1a-8e6f-3d2b9c7a5e1c', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440002', '960289b9-d32d-4f00-8df7-02f4c04a017c', 95.00, 'CONFIRMED', NULL, NULL),

       -- AMANHÃ - Quadra A (Reservas que conflitam com blocked time da manhã)
       ('1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440002', NULL, 85.00, 'CONFIRMED', NULL, NULL),
       ('2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e', 'bce2c206-3b8d-4ce8-b05d-b236cc375d5e', '550e8400-e29b-41d4-a716-446655440006', NULL, 90.00, 'CONFIRMED', NULL, NULL),

       -- AMANHÃ - Quadra B (Reservas que conflitam com blocked time da tarde)
       ('3c4d5e6f-7a8b-4c9d-0e1f-2a3b4c5d6e7f', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440002', NULL, 80.00, 'CONFIRMED', NULL, NULL),
       ('4d5e6f7a-8b9c-4d0e-1f2a-3b4c5d6e7f8a', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440005', NULL, 75.00, 'CONFIRMED', NULL, NULL);

-- Inserir detalhes dos bloqueios de horário (BlockedTimes)
INSERT INTO tb_blocked_times (id, description, blocked_by_admin_id, is_full_day, recurring_blocked_time_id) VALUES
       -- HOJE - Quadra A (1h específica)
       ('f1a2b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c', 'Manutenção preventiva do sistema de iluminação', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),

       -- HOJE - Quadra B (final do dia)
       ('a2b3c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d', 'Reservado para evento especial', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),

       -- AMANHÃ - Quadra A (manhã inteira)
       ('b3c4d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7e', 'Treinamento exclusivo da equipe profissional - manhã', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),

       -- AMANHÃ - Quadra B (tarde inteira)
       ('c4d5e6f7-a8b9-4c0d-1e2f-3a4b5c6d7e8f', 'Treinamento exclusivo da equipe profissional - tarde', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),

       -- PRÓXIMOS 3 DIAS - Quadra A (Bloqueios individuais - para testar futura funcionalidade de recorrência)
       ('d5e6f7a8-b9c0-4d1e-2f3a-4b5c6d7e8f9a', 'Manutenção programada - Dia 1', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),
       ('e6f7a8b9-c0d1-4e2f-3a4b-5c6d7e8f9a0b', 'Manutenção programada - Dia 2', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL),
       ('f7a8b9c0-d1e2-4f3a-4b5c-6d7e8f9a0b1c', 'Manutenção programada - Dia 3', '960289b9-d32d-4f00-8df7-02f4c04a017c', FALSE, NULL);

