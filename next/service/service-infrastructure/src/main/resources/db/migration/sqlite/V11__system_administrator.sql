ALTER TABLE api_principal ADD COLUMN system_role TEXT;

-- Existing installations already have the canonical bootstrap principal. Promote it
-- during upgrade so global control-plane access is not lost when system routes become
-- restricted to an explicit system administrator.
UPDATE api_principal
SET system_role = 'SYSTEM_ADMIN'
WHERE id = 'principal_bootstrap';
