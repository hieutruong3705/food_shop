-- Additive. Promotions and vouchers can carry a banner image the admin uploads, and every banner
-- now says where it is shown. Banners saved before this belong to the home hero, which is what the
-- storefront already rendered them as, so no banner changes place.
ALTER TABLE promotions ADD COLUMN image_url VARCHAR(500) NULL;
ALTER TABLE vouchers ADD COLUMN image_url VARCHAR(500) NULL;
ALTER TABLE banners ADD COLUMN placement VARCHAR(20) NOT NULL DEFAULT 'HOME_HERO';
