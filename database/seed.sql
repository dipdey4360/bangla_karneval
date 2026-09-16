-- Default admin (password: Admin@2026!)
INSERT INTO admin_users (name, email, password_hash, role)
VALUES (
    'Dipankar Tapan',
    'tapandipankar@gmail.com',
    '$2a$12$4hbR9yrx6KIXiJasipPqhu/2pdmR4OErQyaVMWbR6yCJbQI.TKo06',
    'ADMIN'
) ON CONFLICT (email) DO NOTHING;

-- Event config for 2026
INSERT INTO event_config (
    event_year, price_per_person, event_date, event_location,
    about_text, contact_phone, contact_email,
    contact_whatsapp, contact_facebook, contact_instagram
) VALUES (
    2026, 10.00, '2026-05-15',
    'Wesseling Community Center, Germany',
    'Bangla Karneval celebrates Bengali culture with traditional performances, food, and community in the heart of Germany. Join us for music, dance, drama, and delicious Bengali cuisine!',
    '+491234567890',
    'info@banglakarneval.com',
    '+491234567890',
    'https://facebook.com/banglakarneval',
    'https://instagram.com/banglakarneval'
) ON CONFLICT (event_year) DO NOTHING;

-- Sample events for 2026
INSERT INTO events (event_year, category, title, description, is_highlight) VALUES
    (2026, 'DANCE',   'Traditional Bengali Dance',    'Classical dance performance showcasing Bengali heritage and grace.',       true),
    (2026, 'FOOD',    'Bengali Food Festival',        'Authentic Bengali cuisine including sweets, biriyani, and street food.',   true),
    (2026, 'CONCERT', 'Rabindra Sangeet Concert',     'Musical performances of Tagore songs by talented community artists.',     false),
    (2026, 'DRAMA',   'Cultural Drama Performance',   'Engaging Bengali theatre performed by our local drama group.',            false),
    (2026, 'GAMES',   'Community Games & Activities', 'Fun activities for all ages — kids and adults alike!',                    false),
    (2026, 'DRESS',   'Traditional Dress Competition','Showcase beautiful traditional Bengali attire.',                          true)
ON CONFLICT DO NOTHING;
