-- Create only missing programme editions; dates and venues are left for the board to confirm.
INSERT INTO event_editions(programme_id,slug,title,event_year,theme_key,tagline,poster_path,price_per_person)
SELECT p.id,'club-'||p.code||'-'||s.active_event_year,
 CASE WHEN p.code='bangla-noboborsho' THEN 'Noboborsho' ELSE p.name END || ' ' || s.active_event_year,
 s.active_event_year,p.code,
 CASE p.code
 WHEN 'eid' THEN 'Celebrate Eid with warmth, togetherness and shared joy.'
 WHEN 'puja' THEN 'Come together for devotion, tradition and celebration.'
 WHEN 'bangla-noboborsho' THEN 'Welcome the Bengali New Year with colour, music and new beginnings.'
 WHEN 'bbq' THEN 'Enjoy good food, fresh air and great company.'
 ELSE 'Experience a day of Bengali music, food and festive spirit.' END,
 CASE p.code WHEN 'eid' THEN '/assets/images/programmes/Eid.svg'
 WHEN 'puja' THEN '/assets/images/programmes/puja.svg'
 WHEN 'bangla-noboborsho' THEN '/assets/images/programmes/Bangla_Noboborsho.svg'
 WHEN 'bbq' THEN '/assets/images/programmes/bbq.png' ELSE '/assets/images/home_background.jpg' END,0
FROM programmes p CROSS JOIN application_settings s WHERE s.id=1
 AND p.code IN ('eid','puja','bangla-noboborsho','bbq','bangla-karneval')
 AND NOT EXISTS(SELECT 1 FROM event_editions e WHERE e.programme_id=p.id AND e.event_year=s.active_event_year)
ON CONFLICT(slug) DO NOTHING;
UPDATE event_editions e SET tagline=CASE p.code
 WHEN 'eid' THEN 'Celebrate Eid with warmth, togetherness and shared joy.'
 WHEN 'puja' THEN 'Come together for devotion, tradition and celebration.'
 WHEN 'bangla-noboborsho' THEN 'Welcome the Bengali New Year with colour, music and new beginnings.'
 WHEN 'bbq' THEN 'Enjoy good food, fresh air and great company.'
 ELSE 'Experience a day of Bengali music, food and festive spirit.' END
FROM programmes p WHERE p.id=e.programme_id AND p.code IN ('eid','puja','bangla-noboborsho','bbq','bangla-karneval')
 AND (e.tagline IS NULL OR btrim(e.tagline)='' OR e.tagline='Celebrating Bengali culture, heritage & community');
