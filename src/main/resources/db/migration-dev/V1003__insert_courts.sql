-- Inserir quadras de desenvolvimento
INSERT INTO tb_courts (id, name, description, offset_minutes, is_active, created_at)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'Quadra A',
        'Quadra principal para todas as modalidades',
        0,
        true,
        CURRENT_TIMESTAMP),

       ('b2c3d4e5-f6a7-8901-bcde-f12345678901',
        'Quadra B',
        'Quadra secundária para esportes de areia',
        30,
        true,
        CURRENT_TIMESTAMP);

-- Associar modalidades às quadras
INSERT INTO tb_court_modalities (court_id, modality_id)
VALUES
       ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '550e8400-e29b-41d4-a716-446655440002'),
       ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '550e8400-e29b-41d4-a716-446655440005'),
       ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '550e8400-e29b-41d4-a716-446655440006'),

       ('b2c3d4e5-f6a7-8901-bcde-f12345678901', '550e8400-e29b-41d4-a716-446655440002'),
       ('b2c3d4e5-f6a7-8901-bcde-f12345678901', '550e8400-e29b-41d4-a716-446655440006');

