-- The seeded "Assinaturas" category came before the subscription mark (V11 left it unmarked); mark it so its
-- purchases count as subscriptions on the overview and the card totals (IMP-65). The user can still unmark it.
UPDATE category SET subscription = 1 WHERE system_category = 0 AND name IN ('Assinaturas', 'Subscriptions');
