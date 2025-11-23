-- Inserir agendamentos (schedule_entries)
INSERT INTO tb_schedule_entries (id, court_id, date, start_time, end_time, entry_type, created_at) VALUES
       -- HOJE - Quadra A
       ('3e7a9b2c-5d8f-4c3e-9a1b-7f6e4d2c1a0b', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '08:00:00', '09:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('8f1c4e9a-2b7d-4a5c-8e3f-1d6b9c4a7e2f', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '10:00:00', '11:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('c2d5e8f1-9a4b-4c7e-8d3a-5f6b2e1c9a7d', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '14:00:00', '15:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('6a9c2e5f-8b1d-4c7a-9e3b-4f7d2a5c8e1b', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '16:00:00', '17:00:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('d8e1f4a7-9c2b-4d5e-8a6f-3b7c1e9d4a2c', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', CURRENT_DATE, '18:00:00', '19:00:00', 'RESERVATION', CURRENT_TIMESTAMP),

       -- HOJE - Quadra B
       ('b5c8e1f4-7a9d-4c2e-8b6f-3d5a9c7e1b4f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '08:30:00', '09:30:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('e9f2a5c8-1b4d-4c7e-8a3f-6d9b2c5e8a1f', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '10:30:00', '11:30:00', 'RESERVATION', CURRENT_TIMESTAMP),
       ('a4c7e9f2-8b5d-4c1a-8e6f-3d2b9c7a5e1c', 'b2c3d4e5-f6a7-8901-bcde-f12345678901', CURRENT_DATE, '15:00:00', '16:00:00', 'RESERVATION', CURRENT_TIMESTAMP);

-- Inserir detalhes das reservas
INSERT INTO tb_reservations (id, user_id, modality_id, scheduled_by_admin_id, price, status, recurring_reservation_id) VALUES
       -- HOJE - Quadra A
       ('3e7a9b2c-5d8f-4c3e-9a1b-7f6e4d2c1a0b', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440002', NULL, 80.00, 'CONFIRMED', NULL),
       ('8f1c4e9a-2b7d-4a5c-8e3f-1d6b9c4a7e2f', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440005', NULL, 75.00, 'CONFIRMED', NULL),
       ('c2d5e8f1-9a4b-4c7e-8d3a-5f6b2e1c9a7d', 'bce2c206-3b8d-4ce8-b05d-b236cc375d5e', '550e8400-e29b-41d4-a716-446655440006', '960289b9-d32d-4f00-8df7-02f4c04a017c', 90.00, 'CONFIRMED', NULL),
       ('6a9c2e5f-8b1d-4c7a-9e3b-4f7d2a5c8e1b', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440002', NULL, 85.00, 'CONFIRMED', NULL),
       ('d8e1f4a7-9c2b-4d5e-8a6f-3b7c1e9d4a2c', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440005', NULL, 100.00, 'CONFIRMED', NULL),

       -- HOJE - Quadra B
       ('b5c8e1f4-7a9d-4c2e-8b6f-3d5a9c7e1b4f', 'bce2c206-3b8d-4ce8-b05d-b236cc375d5e', '550e8400-e29b-41d4-a716-446655440002', NULL, 70.00, 'CONFIRMED', NULL),
       ('e9f2a5c8-1b4d-4c7e-8a3f-6d9b2c5e8a1f', '228d0d4b-4cce-4d02-a41e-80513ad16310', '550e8400-e29b-41d4-a716-446655440006', NULL, 65.00, 'COMPLETED', NULL),
       ('a4c7e9f2-8b5d-4c1a-8e6f-3d2b9c7a5e1c', '72390b6d-8b2f-4f3d-8abc-e89968ddfa30', '550e8400-e29b-41d4-a716-446655440002', '960289b9-d32d-4f00-8df7-02f4c04a017c', 95.00, 'CONFIRMED', NULL);
