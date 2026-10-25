
-- Seed venues
INSERT INTO venues (venue_id, name, city) VALUES
  ('venue_001', 'Madison Square Garden', 'New York'),
  ('venue_002', 'Barclays Center', 'Brooklyn'),
  ('venue_003', 'United Center', 'Chicago')
    ON CONFLICT (venue_id) DO NOTHING;

-- Seed events
INSERT INTO events (
    event_id,
    name,
    category,
    start_time,
    min_price,
    max_price,
    currency,
    venue_id
) VALUES
      ('evt_001', 'Coldplay Live', 'concert',
       '2026-11-15 19:30:00-05', 85.00, 240.00, 'USD', 'venue_001'),

      ('evt_002', 'NBA Basketball Night', 'sports',
       '2026-11-20 18:00:00-05', 50.00, 180.00, 'USD', 'venue_002'),

      ('evt_003', 'Taylor Swift Tribute', 'concert',
       '2026-12-05 20:00:00-05', 60.00, 150.00, 'USD', 'venue_001'),

      ('evt_004', 'Comedy Festival', 'comedy',
       '2026-12-10 19:00:00-05', 30.00, 100.00, 'USD', 'venue_002'),

      ('evt_005', 'Chicago Bulls Night', 'sports',
       '2026-12-15 19:30:00-06', 75.00, 220.00, 'USD', 'venue_003'),

      ('evt_006', 'Rock Music Festival', 'concert',
       '2027-01-10 18:00:00-05', 90.00, 250.00, 'USD', 'venue_001'),

      ('evt_007', 'Stand-Up Comedy Special', 'comedy',
       '2027-01-20 20:00:00-05', 25.00, 85.00, 'USD', 'venue_002'),

      ('evt_008', 'Basketball Championship', 'sports',
       '2027-02-14 19:00:00-06', 100.00, 350.00, 'USD', 'venue_003'),

      ('evt_009', 'Jazz Night', 'concert',
       '2027-03-05 20:00:00-05', 40.00, 120.00, 'USD', 'venue_001'),

      ('evt_010', 'International Comedy Tour', 'comedy',
       '2027-03-20 19:30:00-04', 35.00, 110.00, 'USD', 'venue_002')
    ON CONFLICT (event_id) DO NOTHING;

-- Seed ticket inventory
INSERT INTO inventory (
    event_id,
    total_tickets,
    available_tickets
) VALUES
      ('evt_001', 1000, 842),
      ('evt_002', 800, 500),
      ('evt_003', 1200, 950),
      ('evt_004', 500, 350),
      ('evt_005', 1000, 700),
      ('evt_006', 1500, 1200),
      ('evt_007', 600, 400),
      ('evt_008', 2000, 1500),
      ('evt_009', 700, 550),
      ('evt_010', 900, 650)
    ON CONFLICT (event_id) DO NOTHING;

-- Sample held reservation for trying reservation confirmation before
-- the reservation API exists
INSERT INTO reservations (
    reservation_id,
    event_id,
    customer_id,
    quantity,
    status,
    expires_at
) VALUES
      ('res_demo_001', 'evt_001', 'cust_001', 2, 'HELD', '2027-12-31 23:59:00-05')
    ON CONFLICT (reservation_id) DO NOTHING;
